package com.pzhgp.backend.service;

import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.repository.BreederRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Odzyskiwanie hasła za pomocą linku wysyłanego na adres e-mail konta.
 * <p>
 * Link zawiera losowy, jednorazowy token. W bazie zapisywany jest tylko jego skrót SHA-256,
 * więc odczytanie bazy nie pozwala nikomu zmienić cudzego hasła. Nowa prośba unieważnia
 * poprzedni link, a wykorzystany albo przeterminowany token przestaje działać.
 */
@Slf4j
@Service
public class PasswordResetService {

    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);

    /** 32 losowe bajty (256 bitów) — tokenu nie da się odgadnąć. */
    private static final int TOKEN_BYTES = 32;

    private final BreederRepository breederRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final Clock clock;
    private final String frontendUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            BreederRepository breederRepository,
            PasswordEncoder passwordEncoder,
            MailService mailService,
            Clock clock,
            @Value("${frontend.url}") String frontendUrl
    ) {
        this.breederRepository = breederRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.clock = clock;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Zapisuje nowy token i wysyła link do zmiany hasła, o ile konto z podanym adresem istnieje.
     * <p>
     * Dla nieznanego adresu metoda nic nie robi i niczego nie zgłasza, żeby odpowiedź serwera
     * nie zdradzała, które adresy e-mail są zarejestrowane.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        Optional<Breeder> account = breederRepository.findByEmailIgnoreCase(email);

        if (account.isEmpty()) {
            return;
        }

        Breeder breeder = account.get();
        String token = generateToken();

        breeder.setPasswordResetTokenHash(hash(token));
        breeder.setPasswordResetTokenExpiresAt(clock.instant().plus(TOKEN_LIFETIME));
        breederRepository.save(breeder);

        mailService.sendPasswordResetEmail(breeder.getEmail(), breeder.getName(), frontendUrl + "/reset-password?token=" + token, TOKEN_LIFETIME.toMinutes());

        // Adres e-mail to dane osobowe, dlatego w logu jest tylko ID konta.
        log.info("Przyjęto prośbę o zmianę hasła dla konta o ID {}.", breeder.getId());
    }

    /**
     * Ustawia nowe hasło konta wskazanego przez token i unieważnia ten token.
     *
     * @throws IllegalArgumentException gdy token jest nieprawidłowy, już wykorzystany albo przeterminowany
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Breeder breeder = breederRepository.findByPasswordResetTokenHash(hash(token))
                .filter(account -> account.getPasswordResetTokenExpiresAt().isAfter(clock.instant()))
                .orElseThrow(() -> new IllegalArgumentException("Link do zmiany hasła jest nieprawidłowy lub wygasł. Poproś o nowy link."));

        breeder.setPasswordHash(passwordEncoder.encode(newPassword));
        breeder.setPasswordResetTokenHash(null);
        breeder.setPasswordResetTokenExpiresAt(null);
        breederRepository.save(breeder);

        log.info("Zmieniono hasło konta o ID {} za pomocą linku z wiadomości e-mail.", breeder.getId());
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algorytm SHA-256 jest niedostępny.", ex);
        }
    }
}