package com.pzhgp.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Wysyłka wiadomości e-mail przez serwer SMTP skonfigurowany we właściwościach {@code spring.mail.*}. */
@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String sender;

    public MailService(JavaMailSender mailSender, @Value("${mail.from}") String sender) {
        this.mailSender = mailSender;
        this.sender = sender;
    }

    /**
     * Wysyła link do ustawienia nowego hasła.
     * <p>
     * Metoda działa w tle: żądanie nie czeka na serwer pocztowy, więc czas odpowiedzi nie zdradza,
     * czy konto z podanym adresem istnieje. Nieudana wysyłka trafia wyłącznie do logu.
     *
     * @param recipient          adres e-mail konta
     * @param name               imię właściciela konta
     * @param resetLink          adres strony z formularzem nowego hasła, zawierający jednorazowy token
     * @param validityInMinutes  czas ważności linku
     */
    @Async
    public void sendPasswordResetEmail(String recipient, String name, String resetLink, long validityInMinutes) {
        send(recipient, "Zmiana hasła w serwisie PZHGP Oddział Żagań", """
                Dzień dobry %s,

                otrzymaliśmy prośbę o zmianę hasła do Twojego konta w serwisie PZHGP Oddział Żagań.
                Aby ustawić nowe hasło, otwórz poniższy link. Link jest ważny %d minut i można go użyć tylko raz.

                %s

                Jeśli to nie Ty prosiłeś o zmianę hasła, zignoruj tę wiadomość. Twoje hasło pozostanie bez zmian.
                """.formatted(name, validityInMinutes, resetLink));
    }

    /**
     * Informuje właściciela konta, że jego hasło zostało zmienione. Jeśli zmiany dokonał ktoś inny,
     * właściciel dowie się o tym i może odzyskać dostęp do konta. Metoda działa w tle, tak jak wysyłka linku.
     *
     * @param recipient adres e-mail konta
     * @param name      imię właściciela konta
     */
    @Async
    public void sendPasswordChangedEmail(String recipient, String name) {
        send(recipient, "Hasło do konta zostało zmienione", """
                Dzień dobry %s,

                hasło do Twojego konta w serwisie PZHGP Oddział Żagań zostało właśnie zmienione.
                Ze względów bezpieczeństwa wylogowaliśmy Cię ze wszystkich urządzeń.

                Jeśli to nie Ty zmieniłeś hasło, od razu ustaw nowe za pomocą opcji „Nie pamiętasz hasła?”
                na stronie logowania i skontaktuj się z administratorem oddziału.
                """.formatted(name));
    }

    // Nieudana wysyłka trafia wyłącznie do logu, bez treści wiadomości i bez adresu odbiorcy.
    private void send(String recipient, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Nie udało się wysłać wiadomości „{}”.", subject, ex);
        }
    }
}