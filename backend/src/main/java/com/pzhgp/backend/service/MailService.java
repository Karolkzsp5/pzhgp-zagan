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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject("Zmiana hasła w serwisie PZHGP Oddział Żagań");
        message.setText("""
                Dzień dobry %s,

                otrzymaliśmy prośbę o zmianę hasła do Twojego konta w serwisie PZHGP Oddział Żagań.
                Aby ustawić nowe hasło, otwórz poniższy link. Link jest ważny %d minut i można go użyć tylko raz.

                %s

                Jeśli to nie Ty prosiłeś o zmianę hasła, zignoruj tę wiadomość. Twoje hasło pozostanie bez zmian.
                """.formatted(name, validityInMinutes, resetLink));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Nie udało się wysłać wiadomości z linkiem do zmiany hasła.", ex);
        }
    }
}