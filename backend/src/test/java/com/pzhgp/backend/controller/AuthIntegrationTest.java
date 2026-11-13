package com.pzhgp.backend.controller;

import com.pzhgp.backend.config.AuthCookieService;
import com.pzhgp.backend.dto.ForgotPasswordRequest;
import com.pzhgp.backend.dto.LoginRequest;
import com.pzhgp.backend.dto.RegistrationRequest;
import com.pzhgp.backend.dto.ResetPasswordRequest;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.Role;
import com.pzhgp.backend.exception.CaptchaUnavailableException;
import com.pzhgp.backend.exception.CaptchaVerificationException;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.service.MailService;
import com.pzhgp.backend.service.TurnstileService;
import com.pzhgp.backend.support.MutableClock;
import com.pzhgp.backend.support.TestClockConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "auth.cookie.secure=false")
@Import(TestClockConfig.class)
@DisplayName("Authorization Integration Tests")
class AuthIntegrationTest {

    private static final String CAPTCHA_TOKEN = "captcha-test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    // Atrapa zamiast zapytań do Cloudflare: domyślnie przyjmuje każdy token.
    @MockitoBean
    private TurnstileService turnstileService;

    // Atrapa zamiast serwera pocztowego: testy odczytują z niej link przekazany do wysyłki.
    @MockitoBean
    private MailService mailService;

    private RegistrationRequest validRequest;

    @BeforeEach
    void setUp() {
        // Limity prób są wspólne dla testów; po przesunięciu zegara o dobę wcześniejsze próby wygasają.
        clock.advance(Duration.ofDays(1));
        validRequest = new RegistrationRequest(
                "Jan", "Kowalski", LocalDate.parse("1995-05-15"), "68-100", "Żagań",
                "Słowackiego", "10", 1L, "integration.test@pzhgp.pl", "444444444", "SecurePass1!", CAPTCHA_TOKEN
        );
    }

    private RegistrationRequest validRequestWithCaptchaToken(String captchaToken) {
        return new RegistrationRequest(
                validRequest.name(), validRequest.surname(), validRequest.dateOfBirth(),
                validRequest.postalCode(), validRequest.city(), validRequest.street(),
                validRequest.houseNumber(), validRequest.sectionId(), validRequest.email(),
                validRequest.phoneNumber(), validRequest.password(), captchaToken
        );
    }

    @Test
    @DisplayName("Should return 201 after successful breeder registration")
    void shouldRegisterUserSuccessfullyIntegration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should report the validation error of the first invalid form field")
    void shouldReportFirstInvalidFieldWhenSeveralAreInvalid() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "Jan", "", LocalDate.parse("1995-05-15"), "68100", "Żagań",
                "Słowackiego", "10", 1L, "not-an-email", "123", "SecurePass1!", CAPTCHA_TOKEN
        );

        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Nazwisko nie może być puste."));
        }
    }

    @Test
    @DisplayName("Should accept any non-alphanumeric character as the special character of a password")
    void shouldAcceptAnyNonAlphanumericSpecialCharacter() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "Jan", "Kowalski", LocalDate.parse("1995-05-15"), "68-100", "Żagań",
                "Słowackiego", "10", 1L, "special.char@pzhgp.pl", "444444446", "Haslo123;", CAPTCHA_TOKEN
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should accept names with letters from other alphabets and apostrophes")
    void shouldRegisterUserWithForeignLettersAndApostrophesIntegration() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "Jürgen", "O'Connor-Müller", LocalDate.parse("1995-05-15"), "68-100", "Żagań",
                "Słowackiego", "10", 1L, "foreign.name@pzhgp.pl", "444444445", "SecurePass1!", CAPTCHA_TOKEN
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Breeder saved = breederRepository.findByEmail("foreign.name@pzhgp.pl").orElseThrow();
        assertEquals("Jürgen", saved.getName());
        assertEquals("O'Connor-Müller", saved.getSurname());
    }

    @Test
    @DisplayName("Should normalize email case and surrounding whitespace during registration")
    void shouldNormalizeEmailDuringRegistrationIntegration() throws Exception {
        String requestJson = """
                {
                  "name": "Jan",
                  "surname": "Kowalski",
                  "dateOfBirth": "1995-05-15",
                  "postalCode": "68-100",
                  "city": "Żagań",
                  "street": "Słowackiego",
                  "houseNumber": "10",
                  "sectionId": 1,
                  "email": "  Integration.MixedCase@PZHGP.PL  ",
                  "phoneNumber": "555666777",
                  "password": "SecurePass1!",
                  "captchaToken": "captcha-test-token"
                }
                """;

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(requestJson)).andExpect(status().isCreated());
        Breeder saved = breederRepository.findByEmail("integration.mixedcase@pzhgp.pl").orElseThrow();
        assertEquals("integration.mixedcase@pzhgp.pl", saved.getEmail());
    }

    @Test
    @DisplayName("Should return 400 for invalid registration data")
    void shouldReturnBadRequestOnInvalidInputData() throws Exception {
        RegistrationRequest invalidRequest = new RegistrationRequest(
                "", "", null, "", "",
                "", "", null, "niepoprawny-email", "", "", CAPTCHA_TOKEN
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject registration when password exceeds 72 UTF-8 bytes")
    void shouldRejectRegistrationPasswordExceeding72Utf8Bytes() throws Exception {
        String tooLongPassword = "Aa1!" + "ą".repeat(35);

        RegistrationRequest request = new RegistrationRequest(
                validRequest.name(), validRequest.surname(), validRequest.dateOfBirth(),
                validRequest.postalCode(), validRequest.city(), validRequest.street(),
                validRequest.houseNumber(), validRequest.sectionId(), validRequest.email(),
                validRequest.phoneNumber(), tooLongPassword, CAPTCHA_TOKEN
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Hasło może mieć maksymalnie 72 bajty (UTF-8)."));
    }

    @Test
    @DisplayName("Should return 409 when registering an existing email address")
    void shouldReturnConflictWhenEmailIsDuplicatedIntegration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        RegistrationRequest duplicateRequest = new RegistrationRequest(
                "Andrzej", "Nowak", LocalDate.parse("1990-01-01"), "68-100", "Żagań",
                "Długa", "5", 1L, "integration.test@pzhgp.pl", "111222333", "OtherPass1!", CAPTCHA_TOKEN
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Użytkownik z podanym adresem e-mail już istnieje."));
    }

    @Test
    @DisplayName("Should return 409 when registering an existing phone number")
    void shouldReturnConflictWhenPhoneNumberIsDuplicatedIntegration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        RegistrationRequest duplicatePhoneRequest = new RegistrationRequest(
                "Andrzej", "Nowak", LocalDate.parse("1990-01-01"), "68-100", "Żagań",
                "Długa", "5", 1L, "inny.mail@pzhgp.pl", "444444444", "InneHaslo1!", CAPTCHA_TOKEN
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicatePhoneRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Użytkownik z podanym numerem telefonu już istnieje."));
    }

    @Test
    @DisplayName("Should return 429 when the registration attempt limit is exceeded")
    void shouldReturnTooManyRequestsAfterRegistrationLimit() throws Exception {
        String clientAddress = "192.0.2.20";

        for (int i = 0; i < 5; i++) {
            RegistrationRequest request = new RegistrationRequest(
                    "Jan",
                    "Kowalski",
                    LocalDate.parse("1995-05-15"),
                    "68-100",
                    "Żagań",
                    "Słowackiego",
                    "10",
                    1L,
                    "rate-limit-" + i + "@pzhgp.pl",
                    "88888888" + i,
                    "SecurePass1!",
                    CAPTCHA_TOKEN
            );

            mockMvc.perform(post("/api/auth/register")
                            .with(httpRequest -> {
                                httpRequest.setRemoteAddr(clientAddress);
                                return httpRequest;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/auth/register")
                        .with(httpRequest -> {
                            httpRequest.setRemoteAddr(clientAddress);
                            return httpRequest;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Zbyt wiele prób rejestracji. Spróbuj ponownie później."));

        // Żądanie ponad limit zostało odrzucone, zanim serwer zapytał Cloudflare.
        verify(turnstileService, times(5)).verifyToken(CAPTCHA_TOKEN);
    }

    @Test
    @DisplayName("Should verify the CAPTCHA token before creating an account")
    void shouldVerifyCaptchaTokenDuringRegistration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        verify(turnstileService).verifyToken(CAPTCHA_TOKEN);
    }

    @Test
    @DisplayName("Should return 400 when the CAPTCHA token is missing")
    void shouldRejectRegistrationWithoutCaptchaToken() throws Exception {
        for (String missingToken : new String[]{null, "   "}) {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestWithCaptchaToken(missingToken))))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Dokończ weryfikację CAPTCHA."));
        }

        verify(turnstileService, never()).verifyToken(any());
        assertFalse(breederRepository.findByEmail(validRequest.email()).isPresent());
    }

    @Test
    @DisplayName("Should return 400 and not create an account when the CAPTCHA token is rejected")
    void shouldRejectRegistrationWhenCaptchaVerificationFails() throws Exception {
        doThrow(new CaptchaVerificationException("Weryfikacja CAPTCHA nie powiodła się. Spróbuj ponownie."))
                .when(turnstileService).verifyToken(CAPTCHA_TOKEN);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Weryfikacja CAPTCHA nie powiodła się. Spróbuj ponownie."));

        assertFalse(breederRepository.findByEmail(validRequest.email()).isPresent());
    }

    @Test
    @DisplayName("Should return 503 and not create an account when the CAPTCHA service is unavailable")
    void shouldReturnServiceUnavailableWhenCaptchaCannotBeVerified() throws Exception {
        doThrow(new CaptchaUnavailableException("Weryfikacja CAPTCHA jest chwilowo niedostępna. Spróbuj ponownie później."))
                .when(turnstileService).verifyToken(CAPTCHA_TOKEN);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Weryfikacja CAPTCHA jest chwilowo niedostępna. Spróbuj ponownie później."));

        assertFalse(breederRepository.findByEmail(validRequest.email()).isPresent());
    }

    @Test
    @DisplayName("Should send a password reset link and accept the new password at login")
    void shouldResetPasswordWithLinkFromEmail() throws Exception {
        createActiveBreeder();

        requestPasswordReset(validRequest.email()).andExpect(status().isNoContent()).andExpect(content().string(""));

        resetPassword(sentResetToken(), "NoweHaslo1!").andExpect(status().isNoContent());

        loginWithCookieJson(objectMapper.writeValueAsString(new LoginRequest(validRequest.email(), validRequest.password(), false)), null)
                .andExpect(status().isUnauthorized());
        loginWithCookieJson(objectMapper.writeValueAsString(new LoginRequest(validRequest.email(), "NoweHaslo1!", false)), null)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should log out other devices after the password is reset with the e-mail link")
    void shouldEndSessionsAfterPasswordReset() throws Exception {
        createActiveBreeder();
        Cookie otherDeviceCookie = loginWithCookie(obtainCsrf(), false);
        mockMvc.perform(get("/api/auth/me").cookie(otherDeviceCookie)).andExpect(status().isOk());

        requestPasswordReset(validRequest.email()).andExpect(status().isNoContent());
        resetPassword(sentResetToken(), "NoweHaslo1!").andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").cookie(otherDeviceCookie)).andExpect(status().isUnauthorized());
        verify(mailService).sendPasswordChangedEmail(validRequest.email(), validRequest.name());
    }

    @Test
    @DisplayName("Should give the same answer and send nothing for an unknown e-mail address")
    void shouldNotRevealUnknownEmailDuringPasswordReset() throws Exception {
        requestPasswordReset("nieznany@pzhgp.pl").andExpect(status().isNoContent()).andExpect(content().string(""));

        verify(turnstileService).verifyToken(CAPTCHA_TOKEN);
        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("Should reject a password reset link that was already used")
    void shouldRejectUsedPasswordResetLink() throws Exception {
        createActiveBreeder();
        requestPasswordReset(validRequest.email()).andExpect(status().isNoContent());
        String token = sentResetToken();

        resetPassword(token, "NoweHaslo1!").andExpect(status().isNoContent());

        resetPassword(token, "InneHaslo2!")
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Link do zmiany hasła jest nieprawidłowy lub wygasł. Poproś o nowy link."));
    }

    @Test
    @DisplayName("Should reject an expired password reset link")
    void shouldRejectExpiredPasswordResetLink() throws Exception {
        createActiveBreeder();
        requestPasswordReset(validRequest.email()).andExpect(status().isNoContent());
        String token = sentResetToken();

        clock.advance(Duration.ofMinutes(30));

        resetPassword(token, "NoweHaslo1!")
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Link do zmiany hasła jest nieprawidłowy lub wygasł. Poproś o nowy link."));

        loginWithCookieJson(objectMapper.writeValueAsString(new LoginRequest(validRequest.email(), validRequest.password(), false)), null)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should invalidate the previous password reset link when a new one is requested")
    void shouldInvalidatePreviousPasswordResetLink() throws Exception {
        createActiveBreeder();
        requestPasswordReset(validRequest.email()).andExpect(status().isNoContent());
        String firstToken = sentResetToken();

        requestPasswordReset(validRequest.email()).andExpect(status().isNoContent());
        String secondToken = sentResetToken();

        resetPassword(firstToken, "NoweHaslo1!").andExpect(status().isBadRequest());
        resetPassword(secondToken, "NoweHaslo1!").andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should reject a new password that breaks the password rules and keep the link valid")
    void shouldRejectWeakPasswordDuringPasswordReset() throws Exception {
        createActiveBreeder();
        requestPasswordReset(validRequest.email()).andExpect(status().isNoContent());
        String token = sentResetToken();

        resetPassword(token, "slabe")
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Hasło musi mieć min. 8 znaków, zawierać dużą i małą literę, cyfrę oraz znak specjalny."));

        resetPassword(token, "Aa1!" + "ą".repeat(35))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Hasło może mieć maksymalnie 72 bajty (UTF-8)."));

        resetPassword(token, "NoweHaslo1!").andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 400 and send nothing when the password reset request has no CAPTCHA token")
    void shouldRejectPasswordResetRequestWithoutCaptchaToken() throws Exception {
        createActiveBreeder();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(validRequest.email(), null))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Dokończ weryfikację CAPTCHA."));

        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("Should return 429 when the password reset attempt limit is exceeded")
    void shouldReturnTooManyRequestsAfterPasswordResetLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            requestPasswordReset("nieznany-" + i + "@pzhgp.pl").andExpect(status().isNoContent());
        }

        requestPasswordReset("nieznany@pzhgp.pl")
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Zbyt wiele prób odzyskiwania hasła. Spróbuj ponownie później."));

        // Ustawienie nowego hasła korzysta z tego samego limitu.
        resetPassword("dowolny-token", "NoweHaslo1!").andExpect(status().isTooManyRequests());

        verify(turnstileService, times(5)).verifyToken(CAPTCHA_TOKEN);
    }

    @Test
    @DisplayName("Should authenticate an active user with an HttpOnly cookie")
    void shouldLoginSuccessfullyWithCookieIntegration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        Breeder savedBreeder = breederRepository.findByEmail(validRequest.email()).get();
        savedBreeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.save(savedBreeder);

        Cookie authCookie = loginWithCookie(obtainCsrf(), false);
        mockMvc.perform(get("/api/notifications/unread-count").cookie(authCookie)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow login with an email address in different letter case")
    void shouldLoginWithCaseInsensitiveEmailIntegration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        Breeder savedBreeder = breederRepository.findByEmail(validRequest.email()).orElseThrow();
        savedBreeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.save(savedBreeder);

        String loginJson = """
                {
                  "email": "  INTEGRATION.TEST@PZHGP.PL  ",
                  "password": "SecurePass1!"
                }
                """;

        loginWithCookieJson(loginJson, null).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 401 when logging in with an unknown email address")
    void shouldReturnUnauthorizedOnInvalidLogin() throws Exception {
        LoginRequest invalidLogin = new LoginRequest("doesntexist@test.pl", "BadPassword123", false);

        loginWithCookieJson(objectMapper.writeValueAsString(invalidLogin), null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when logging in with an incorrect password")
    void shouldReturnUnauthorizedOnWrongPasswordForExistingUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        Breeder savedBreeder = breederRepository.findByEmail(validRequest.email()).get();
        savedBreeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.save(savedBreeder);

        LoginRequest wrongPassLogin = new LoginRequest(validRequest.email(), "ZleHaslo999!", false);

        loginWithCookieJson(objectMapper.writeValueAsString(wrongPassLogin), null)
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Nieprawidłowy adres e-mail lub hasło."));
    }

    @Test
    @DisplayName("Should reject login when password exceeds 72 UTF-8 bytes")
    void shouldRejectLoginPasswordExceeding72Utf8Bytes() throws Exception {
        LoginRequest request = new LoginRequest("test@test.pl", "Aa1!" + "a".repeat(69), false);

        loginWithCookieJson(objectMapper.writeValueAsString(request), null)
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Hasło może mieć maksymalnie 72 bajty (UTF-8)."));
    }

    @Test
    @DisplayName("Should return 403 when logging in to a pending account")
    void shouldReturnForbiddenWhenLoggingInWithPendingAccount() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(validRequest.email(), validRequest.password(), false);

        loginWithCookieJson(objectMapper.writeValueAsString(loginRequest), null)
                .andExpect(status().isForbidden())
                .andExpect(content().string("Twoje konto oczekuje jeszcze na akceptację administratora."));
    }

    @Test
    @DisplayName("Should return 429 when the login attempt limit is exceeded")
    void shouldReturnTooManyRequestsAfterLoginLimit() throws Exception {
        String clientAddress = "192.0.2.10";
        LoginRequest request = new LoginRequest("unknown@test.pl", "WrongPassword123!", false);

        for (int i = 0; i < 30; i++) {
            loginWithCookieJson(objectMapper.writeValueAsString(request), clientAddress).andExpect(status().isUnauthorized());
        }

        loginWithCookieJson(objectMapper.writeValueAsString(request), clientAddress)
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Zbyt wiele prób logowania. Spróbuj ponownie później."));
    }

    @Test
    @DisplayName("Should return 401 when accessing a protected endpoint with an invalid JWT")
    void shouldRejectInvalidJwtOnProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count")
                        .cookie(new Cookie(AuthCookieService.COOKIE_NAME, "invalid.jwt.token")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Wymagane jest uwierzytelnienie."));
    }

    @Test
    @DisplayName("Should return current user data from the database instead of JWT claims")
    void shouldReturnCurrentUserFromDatabase() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        Breeder breeder = breederRepository.findByEmail(validRequest.email()).orElseThrow();
        breeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.saveAndFlush(breeder);

        Cookie authCookie = loginWithCookie(obtainCsrf(), false);

        // Dane zostały zmienione po wystawieniu JWT: /me musi odczytać stan bazy.
        breeder.setName("Piotr");
        breeder.setRole(Role.MODERATOR);
        breederRepository.saveAndFlush(breeder);

        mockMvc.perform(get("/api/auth/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(breeder.getId().intValue()))
                .andExpect(jsonPath("$.email").value(validRequest.email()))
                .andExpect(jsonPath("$.name").value("Piotr"))
                .andExpect(jsonPath("$.role").value("MODERATOR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Should return 401 for current user endpoint without JWT")
    void shouldRejectCurrentUserRequestWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Wymagane jest uwierzytelnienie."));
    }

    @Test
    @DisplayName("Should reject current user request when the account is blocked after login")
    void shouldRejectCurrentUserRequestForBlockedAccount() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        Breeder breeder = breederRepository.findByEmail(validRequest.email()).orElseThrow();
        breeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.saveAndFlush(breeder);

        Cookie authCookie = loginWithCookie(obtainCsrf(), false);

        breeder.setStatus(AccountStatus.BLOCKED);
        breederRepository.saveAndFlush(breeder);

        mockMvc.perform(get("/api/auth/me").cookie(authCookie)).andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions loginWithCookieJson(
            String json, String remoteAddress) throws Exception {
        CsrfValues csrf = obtainCsrf();
        var request = post("/api/auth/login")
                .cookie(csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
        if (remoteAddress != null) {
            request.with(httpRequest -> {
                httpRequest.setRemoteAddr(remoteAddress);
                return httpRequest;
            });
        }
        return mockMvc.perform(request);
    }

    private ResultActions requestPasswordReset(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email, CAPTCHA_TOKEN))));
    }

    private ResultActions resetPassword(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest(token, password))));
    }

    /** Token z linku w ostatniej wiadomości przekazanej do wysyłki. */
    private String sentResetToken() {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendPasswordResetEmail(eq(validRequest.email()), eq(validRequest.name()), link.capture(), eq(30L));

        String marker = "/reset-password?token=";
        return link.getValue().substring(link.getValue().indexOf(marker) + marker.length());
    }

    private void createActiveBreeder() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
        Breeder breeder = breederRepository.findByEmail(validRequest.email()).orElseThrow();
        breeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.saveAndFlush(breeder);
    }

    private record CsrfValues(Cookie cookie, String token) {}

    private CsrfValues obtainCsrf() throws Exception {
        var response = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isNoContent()).andReturn().getResponse();

        String setCookie = response.getHeaders(HttpHeaders.SET_COOKIE).stream().filter(header -> header.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow();
        String cookieToken = setCookie.substring("XSRF-TOKEN=".length()).split(";", 2)[0];
        return new CsrfValues(new Cookie("XSRF-TOKEN", cookieToken), cookieToken);
    }

    private Cookie loginWithCookie(CsrfValues csrf, boolean rememberMe) throws Exception {
        var response = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(validRequest.email(), validRequest.password(), rememberMe))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andReturn().getResponse();

        String setCookie = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(header -> header.startsWith(AuthCookieService.COOKIE_NAME + "="))
                .findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("HttpOnly"));
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("SameSite=Lax"));
        if (rememberMe) {
            org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("Max-Age=86400"));
        } else {
            org.junit.jupiter.api.Assertions.assertFalse(setCookie.contains("Max-Age="));
        }
        String token = setCookie.substring((AuthCookieService.COOKIE_NAME + "=").length()).split(";", 2)[0];
        return new Cookie(AuthCookieService.COOKIE_NAME, token);
    }

    @Test
    @DisplayName("Should issue an HttpOnly authentication cookie for persistent login")
    void shouldIssueHttpOnlyCookieForPersistentLogin() throws Exception {
        createActiveBreeder();
        Cookie authCookie = loginWithCookie(obtainCsrf(), true);

        mockMvc.perform(get("/api/auth/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(validRequest.email()));
    }

    @Test
    @DisplayName("Should issue a session authentication cookie when remember me is disabled")
    void shouldIssueSessionCookieWithoutRememberMe() throws Exception {
        createActiveBreeder();
        Cookie authCookie = loginWithCookie(obtainCsrf(), false);

        mockMvc.perform(get("/api/notifications/unread-count").cookie(authCookie)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject cookie login without a CSRF token")
    void shouldRejectCookieLoginWithoutCsrf() throws Exception {
        createActiveBreeder();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(validRequest.email(), validRequest.password(), false))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should block state-changing cookie requests without a CSRF token")
    void shouldRejectCookieAuthenticatedChangesWithoutCsrf() throws Exception {
        createActiveBreeder();
        Cookie authCookie = loginWithCookie(obtainCsrf(), false);

        mockMvc.perform(put("/api/notifications/123456789/read").cookie(authCookie)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject a Bearer token without an authentication cookie")
    void shouldRejectBearerAuthenticationWithoutCookie() throws Exception {
        createActiveBreeder();
        Cookie authCookie = loginWithCookie(obtainCsrf(), false);

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authCookie.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should not expose the JWT anywhere except the HttpOnly cookie after logging in")
    void shouldNotExposeTokenOutsideTheCookie() throws Exception {
        createActiveBreeder();
        CsrfValues csrf = obtainCsrf();

        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(validRequest.email(), validRequest.password(), false))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION));
    }

    @Test
    @DisplayName("Should not register the old cookie login address")
    void shouldNotRegisterOldLoginCookieEndpoint() {
        boolean oldEndpointRegistered = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                .anyMatch(mapping -> mapping.getPatternValues().contains("/api/auth/login-cookie"));

        assertFalse(oldEndpointRegistered);
    }

    @Test
    @DisplayName("Should accept state-changing cookie requests with a valid CSRF token")
    void shouldAcceptCookieAuthenticatedChangesWithCsrf() throws Exception {
        createActiveBreeder();
        CsrfValues csrf = obtainCsrf();
        Cookie authCookie = loginWithCookie(csrf, false);

        // Token CSRF został zaakceptowany; nieistniejący zasób zwraca 404 zamiast 403.
        mockMvc.perform(put("/api/notifications/123456789/read")
                        .cookie(authCookie, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reject state-changing cookie requests with an invalid CSRF token")
    void shouldRejectCookieAuthenticatedChangesWithInvalidCsrf() throws Exception {
        createActiveBreeder();
        CsrfValues csrf = obtainCsrf();
        Cookie authCookie = loginWithCookie(csrf, false);

        mockMvc.perform(put("/api/notifications/123456789/read")
                        .cookie(authCookie, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token() + "-invalid"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should require CSRF even when an Authorization header accompanies the cookie")
    void shouldRequireCsrfWhenAuthCookieIsPresentWithAuthorizationHeader() throws Exception {
        createActiveBreeder();
        Cookie authCookie = loginWithCookie(obtainCsrf(), false);

        mockMvc.perform(put("/api/notifications/123456789/read")
                        .cookie(authCookie)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authCookie.getValue()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject logout without a CSRF token")
    void shouldRejectLogoutWithoutCsrf() throws Exception {
        createActiveBreeder();
        CsrfValues csrf = obtainCsrf();
        Cookie authCookie = loginWithCookie(csrf, false);

        mockMvc.perform(post("/api/auth/logout").cookie(authCookie, csrf.cookie())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should clear authentication cookie after CSRF-protected logout")
    void shouldLogoutAndClearAuthCookie() throws Exception {
        createActiveBreeder();
        CsrfValues csrf = obtainCsrf();
        Cookie authCookie = loginWithCookie(csrf, false);

        var response = mockMvc.perform(post("/api/auth/logout")
                        .cookie(authCookie, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();
        org.junit.jupiter.api.Assertions.assertTrue(response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .anyMatch(header -> header.startsWith(AuthCookieService.COOKIE_NAME + "=") && header.contains("Max-Age=0")));
    }
}