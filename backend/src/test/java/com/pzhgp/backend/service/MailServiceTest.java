package com.pzhgp.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mail Service Unit Tests")
class MailServiceTest {

    private static final String RESET_LINK = "http://localhost:3000/reset-password?token=abc123";

    @Mock
    private JavaMailSender mailSender;

    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new MailService(mailSender, "no-reply@pzhgp.pl");
    }

    @Test
    @DisplayName("Should send the password reset link to the account address")
    void shouldSendPasswordResetEmail() {
        mailService.sendPasswordResetEmail("jan.kowalski@test.pl", "Jan", RESET_LINK, 30);

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());

        assertEquals("no-reply@pzhgp.pl", message.getValue().getFrom());
        assertArrayEquals(new String[]{"jan.kowalski@test.pl"}, message.getValue().getTo());
        assertEquals("Zmiana hasła w serwisie PZHGP Oddział Żagań", message.getValue().getSubject());

        String text = message.getValue().getText();
        assertNotNull(text);
        assertTrue(text.startsWith("Dzień dobry Jan,"));
        assertTrue(text.contains(RESET_LINK));
        assertTrue(text.contains("Link jest ważny 30 minut"));
    }

    @Test
    @DisplayName("Should not fail the request when the mail server rejects the message")
    void shouldNotThrowWhenSendingFails() {
        doThrow(new MailSendException("SMTP server is unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> mailService.sendPasswordResetEmail("jan.kowalski@test.pl", "Jan", RESET_LINK, 30));
    }

    @Test
    @DisplayName("Should inform the account owner that the password was changed")
    void shouldSendPasswordChangedEmail() {
        mailService.sendPasswordChangedEmail("jan.kowalski@test.pl", "Jan");

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());

        assertArrayEquals(new String[]{"jan.kowalski@test.pl"}, message.getValue().getTo());
        assertEquals("Hasło do konta zostało zmienione", message.getValue().getSubject());

        String text = message.getValue().getText();
        assertNotNull(text);
        assertTrue(text.startsWith("Dzień dobry Jan,"));
        assertTrue(text.contains("wylogowaliśmy Cię ze wszystkich urządzeń"));
        assertTrue(text.contains("Nie pamiętasz hasła?"));
    }
}