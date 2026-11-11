package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.TurnstileVerifyResponse;
import com.pzhgp.backend.exception.CaptchaUnavailableException;
import com.pzhgp.backend.exception.CaptchaVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Set;

/**
 * Weryfikacja tokenów CAPTCHA w zewnętrznym API Cloudflare Turnstile.
 * <p>
 * Widżet Turnstile w przeglądarce wydaje token po sprawdzeniu, że formularz wypełnia człowiek.
 * Sam token niczego nie dowodzi, dopóki serwer nie potwierdzi go w Cloudflare: dopiero odpowiedź
 * endpointu siteverify mówi, czy token jest prawdziwy, niewykorzystany i nieprzeterminowany.
 * Token jest jednorazowy i ważny 5 minut. Klucz tajny zna wyłącznie backend.
 */
@Slf4j
@Service
public class TurnstileService {

    /** Kody błędów oznaczające problem po stronie serwera (konfiguracja albo Cloudflare), a nie błędny token. */
    private static final Set<String> SERVER_ERROR_CODES = Set.of("missing-input-secret", "invalid-input-secret", "bad-request", "internal-error");

    private static final String UNAVAILABLE_MESSAGE = "Weryfikacja CAPTCHA jest chwilowo niedostępna. Spróbuj ponownie później.";

    private final RestClient restClient;
    private final String verifyUrl;
    private final String secretKey;

    public TurnstileService(
            RestClient.Builder restClientBuilder,
            @Value("${turnstile.verify-url}") String verifyUrl,
            @Value("${turnstile.secret-key}") String secretKey
    ) {
        this.restClient = restClientBuilder.build();
        this.verifyUrl = verifyUrl;
        this.secretKey = secretKey;
    }

    /**
     * Sprawdza token otrzymany z widżetu Turnstile.
     *
     * @throws CaptchaVerificationException gdy token jest nieprawidłowy, już wykorzystany albo przeterminowany
     * @throws CaptchaUnavailableException  gdy nie udało się uzyskać poprawnej odpowiedzi z Cloudflare
     */
    public void verifyToken(String token) {
        // Klucz tajny trafia do treści żądania, a nie do adresu, więc nie pojawi się w logach ani w komunikatach wyjątków.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", token);

        TurnstileVerifyResponse response;
        try {
            response = restClient.post()
                    .uri(verifyUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TurnstileVerifyResponse.class);
        } catch (RestClientException ex) {
            log.error("Nie udało się sprawdzić tokenu CAPTCHA w Cloudflare Turnstile.", ex);
            throw new CaptchaUnavailableException(UNAVAILABLE_MESSAGE);
        }

        if (response == null) {
            log.error("Cloudflare Turnstile zwrócił pustą odpowiedź.");
            throw new CaptchaUnavailableException(UNAVAILABLE_MESSAGE);
        }

        if (response.success()) {
            return;
        }

        if (response.errorCodes().stream().anyMatch(SERVER_ERROR_CODES::contains)) {
            // Najczęstsza przyczyna to błędna wartość TURNSTILE_SECRET_KEY (kod invalid-input-secret).
            log.error("Cloudflare Turnstile odrzucił zapytanie serwera (kody błędów: {}).", response.errorCodes());
            throw new CaptchaUnavailableException(UNAVAILABLE_MESSAGE);
        }

        throw new CaptchaVerificationException("Weryfikacja CAPTCHA nie powiodła się. Spróbuj ponownie.");
    }
}