package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.LoginRequest;
import com.pzhgp.backend.dto.RegistrationRequest;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.repository.BreederRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BreederServiceTest {

    @Mock
    private BreederRepository breederRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private BreederService breederService;

    private RegistrationRequest validRegistrationRequest;
    private Breeder activeBreeder;

    @BeforeEach
    void setUp() {
        validRegistrationRequest = new RegistrationRequest(
                "Tomasz", "Nowak", LocalDate.parse("2003-01-12"), "11-111", "Test",
                "Testowa", "15b", 1, "testcypress@test.com", "444444444", "Testcypress1@"
        );

        activeBreeder = new Breeder();
        activeBreeder.setId(1L);
        activeBreeder.setEmail("testcypress@test.com");
        activeBreeder.setPasswordHash("hashedPassword123");
        activeBreeder.setStatus(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should throw an exception if the email address already exists in the database")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(breederRepository.existsByEmail(validRegistrationRequest.email())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> breederService.registerNewBreeder(validRegistrationRequest));

        assertEquals("Użytkownik z podanym adresem e-mail już istnieje.", ex.getMessage());
        verify(breederRepository, never()).save(any(Breeder.class));
    }

    @Test
    @DisplayName("Should throw an exception if the phone number already exists in the database")
    void shouldThrowExceptionWhenPhoneNumberAlreadyExists() {
        when(breederRepository.existsByEmail(validRegistrationRequest.email())).thenReturn(false);
        when(breederRepository.existsByPhoneNumber(validRegistrationRequest.phoneNumber())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> breederService.registerNewBreeder(validRegistrationRequest));

        assertEquals("Użytkownik z podanym numerem telefonu już istnieje.", ex.getMessage());
        verify(breederRepository, never()).save(any(Breeder.class));
    }

    @Test
    @DisplayName("Should successfully register a new breeder")
    void shouldRegisterNewBreederSuccessfully() {
        when(breederRepository.existsByEmail(validRegistrationRequest.email())).thenReturn(false);
        when(breederRepository.existsByPhoneNumber(validRegistrationRequest.phoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(validRegistrationRequest.password())).thenReturn("hashedPassword123");

        assertDoesNotThrow(() -> breederService.registerNewBreeder(validRegistrationRequest));

        verify(breederRepository, times(1)).save(any(Breeder.class));
    }

    @Test
    @DisplayName("Should throw an exception when logging in with a non-existent email address")
    void shouldThrowExceptionWhenLoginEmailDoesNotExist() {
        LoginRequest loginRequest = new LoginRequest("nieistnieje@test.com", "Testcypress1@");

        when(breederRepository.findByEmail(loginRequest.email())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> breederService.login(loginRequest));

        assertEquals("Nieprawidłowy adres e-mail lub hasło.", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw an exception when logging in with an incorrect password")
    void shouldThrowExceptionWhenLoginPasswordIsIncorrect() {
        LoginRequest loginRequest = new LoginRequest("testcypress@test.com", "ZleHaslo123");

        when(breederRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(activeBreeder));
        when(passwordEncoder.matches(loginRequest.password(), activeBreeder.getPasswordHash())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> breederService.login(loginRequest));

        assertEquals("Nieprawidłowy adres e-mail lub hasło.", ex.getMessage());
    }

    @Test
    @DisplayName("Should block logins for the account marked as “PENDING”")
    void shouldThrowExceptionWhenLoggingInWithPendingAccount() {
        LoginRequest loginRequest = new LoginRequest("testcypress@test.com", "Testcypress1@");
        activeBreeder.setStatus(AccountStatus.PENDING);

        when(breederRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(activeBreeder));
        when(passwordEncoder.matches(loginRequest.password(), activeBreeder.getPasswordHash())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> breederService.login(loginRequest));

        assertEquals("Twoje konto oczekuje jeszcze na akceptację administratora.", ex.getMessage());
    }

    @Test
    @DisplayName("Should log in successfully and return a JWT token")
    void shouldLoginSuccessfullyAndReturnToken() {
        LoginRequest loginRequest = new LoginRequest("testcypress@test.com", "Testcypress1@");

        when(breederRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(activeBreeder));
        when(passwordEncoder.matches(loginRequest.password(), activeBreeder.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(activeBreeder)).thenReturn("mocked-jwt-token");

        String token = breederService.login(loginRequest);

        assertEquals("mocked-jwt-token", token);
    }
}