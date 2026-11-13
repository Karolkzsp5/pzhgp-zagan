package com.pzhgp.backend.service;

import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Password Reset Service Unit Tests")
class PasswordResetServiceTest {

    private static final String EMAIL = "jan.kowalski@test.pl";
    private static final String LINK_PREFIX = "http://localhost:3000/reset-password?token=";

    @Mock
    private BreederRepository breederRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    private final MutableClock clock = new MutableClock();

    private PasswordResetService passwordResetService;
    private Breeder breeder;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(breederRepository, passwordEncoder, mailService, clock, "http://localhost:3000");

        breeder = new Breeder();
        breeder.setId(1L);
        breeder.setName("Jan");
        breeder.setEmail(EMAIL);
        breeder.setPasswordHash("old-hash");
    }

    /** Prosi o zmianę hasła i zwraca token z linku przekazanego do wysyłki. */
    private String requestResetAndReadToken() {
        when(breederRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(breeder));

        passwordResetService.requestPasswordReset(EMAIL);

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendPasswordResetEmail(eq(EMAIL), eq("Jan"), link.capture(), eq(30L));
        assertTrue(link.getValue().startsWith(LINK_PREFIX));

        return link.getValue().substring(LINK_PREFIX.length());
    }

    private void repositoryFindsBreederByStoredToken() {
        when(breederRepository.findByPasswordResetTokenHash(breeder.getPasswordResetTokenHash())).thenReturn(Optional.of(breeder));
    }

    @Test
    @DisplayName("Should send a reset link and store only the hash of its token")
    void shouldSendLinkAndStoreTokenHash() {
        String token = requestResetAndReadToken();

        assertEquals(43, token.length());
        assertTrue(breeder.getPasswordResetTokenHash().matches("[0-9a-f]{64}"));
        assertNotEquals(token, breeder.getPasswordResetTokenHash());
        assertEquals(clock.instant().plus(Duration.ofMinutes(30)), breeder.getPasswordResetTokenExpiresAt());
        verify(breederRepository).save(breeder);
    }

    @Test
    @DisplayName("Should issue a new token for every request")
    void shouldIssueNewTokenForEveryRequest() {
        String firstToken = requestResetAndReadToken();
        String firstHash = breeder.getPasswordResetTokenHash();

        String secondToken = requestResetAndReadToken();

        assertNotEquals(firstToken, secondToken);
        assertNotEquals(firstHash, breeder.getPasswordResetTokenHash());
    }

    @Test
    @DisplayName("Should do nothing for an unknown e-mail address")
    void shouldIgnoreUnknownEmail() {
        when(breederRepository.findByEmailIgnoreCase("unknown@test.pl")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> passwordResetService.requestPasswordReset("unknown@test.pl"));

        verify(breederRepository, never()).save(any());
        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("Should change the password and invalidate the token")
    void shouldChangePasswordAndInvalidateToken() {
        String token = requestResetAndReadToken();
        repositoryFindsBreederByStoredToken();
        when(passwordEncoder.encode("NoweHaslo1!")).thenReturn("new-hash");

        passwordResetService.resetPassword(token, "NoweHaslo1!");

        assertEquals("new-hash", breeder.getPasswordHash());
        assertNull(breeder.getPasswordResetTokenHash());
        assertNull(breeder.getPasswordResetTokenExpiresAt());
        assertEquals(1, breeder.getSessionVersion());
        verify(mailService).sendPasswordChangedEmail(EMAIL, "Jan");
    }

    @Test
    @DisplayName("Should accept a token just before it expires")
    void shouldAcceptTokenBeforeExpiry() {
        String token = requestResetAndReadToken();
        repositoryFindsBreederByStoredToken();
        when(passwordEncoder.encode("NoweHaslo1!")).thenReturn("new-hash");

        clock.advance(Duration.ofMinutes(30).minusSeconds(1));

        assertDoesNotThrow(() -> passwordResetService.resetPassword(token, "NoweHaslo1!"));
    }

    @Test
    @DisplayName("Should reject an expired token")
    void shouldRejectExpiredToken() {
        String token = requestResetAndReadToken();
        repositoryFindsBreederByStoredToken();

        clock.advance(Duration.ofMinutes(30));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> passwordResetService.resetPassword(token, "NoweHaslo1!"));
        assertEquals("Link do zmiany hasła jest nieprawidłowy lub wygasł. Poproś o nowy link.", exception.getMessage());
        assertEquals("old-hash", breeder.getPasswordHash());
        assertEquals(0, breeder.getSessionVersion());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Should reject an unknown token")
    void shouldRejectUnknownToken() {
        when(breederRepository.findByPasswordResetTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> passwordResetService.resetPassword("unknown-token", "NoweHaslo1!"));

        verifyNoInteractions(passwordEncoder);
    }
}