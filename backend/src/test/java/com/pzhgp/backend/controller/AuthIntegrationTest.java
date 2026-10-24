package com.pzhgp.backend.controller;

import tools.jackson.databind.json.JsonMapper;
import com.pzhgp.backend.config.AuthCookieService;
import jakarta.servlet.http.Cookie;
import com.pzhgp.backend.dto.LoginRequest;
import com.pzhgp.backend.dto.RegistrationRequest;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.Role;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.service.AuthRateLimiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "auth.cookie.secure=false")
@DisplayName("Authorization Integration Tests")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private AuthRateLimiter authRateLimiter;

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private RegistrationRequest validRequest;

    @BeforeEach
    void setUp() {
        authRateLimiter.reset();
        validRequest = new RegistrationRequest(
                "Jan", "Kowalski", LocalDate.parse("1995-05-15"), "68-100", "Żagań",
                "Słowackiego", "10", 1, "integration.test@pzhgp.pl", "444444444", "SecurePass1!"
        );
    }

    @AfterEach
    void tearDown() {
        authRateLimiter.reset();
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
                  "password": "SecurePass1!"
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
                "", "", null, "niepoprawny-email", "", ""
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
                validRequest.phoneNumber(), tooLongPassword
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
                "Długa", "5", 1, "integration.test@pzhgp.pl", "111222333", "OtherPass1!"
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
                "Długa", "5", 1, "inny.mail@pzhgp.pl", "444444444", "InneHaslo1!"
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
                    1,
                    "rate-limit-" + i + "@pzhgp.pl",
                    "88888888" + i,
                    "SecurePass1!"
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
        mockMvc.perform(get("/api/notifications/unread-count").cookie(authCookie))
                .andExpect(status().isOk());
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
        LoginRequest invalidLogin = new LoginRequest("doesntexist@test.pl", "BadPassword123");

        loginWithCookieJson(objectMapper.writeValueAsString(invalidLogin), null)
                .andExpect(status().isUnauthorized());
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

        LoginRequest wrongPassLogin = new LoginRequest(validRequest.email(), "ZleHaslo999!");

        loginWithCookieJson(objectMapper.writeValueAsString(wrongPassLogin), null)
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Nieprawidłowy adres e-mail lub hasło."));
    }

    @Test
    @DisplayName("Should reject login when password exceeds 72 UTF-8 bytes")
    void shouldRejectLoginPasswordExceeding72Utf8Bytes() throws Exception {
        LoginRequest request = new LoginRequest("test@test.pl", "Aa1!" + "a".repeat(69));

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

        LoginRequest loginRequest = new LoginRequest(validRequest.email(), validRequest.password());

        loginWithCookieJson(objectMapper.writeValueAsString(loginRequest), null)
                .andExpect(status().isForbidden())
                .andExpect(content().string("Twoje konto oczekuje jeszcze na akceptację administratora."));
    }

    @Test
    @DisplayName("Should return 429 when the login attempt limit is exceeded")
    void shouldReturnTooManyRequestsAfterLoginLimit() throws Exception {
        String clientAddress = "192.0.2.10";
        LoginRequest request = new LoginRequest("unknown@test.pl", "WrongPassword123!");

        for (int i = 0; i < 30; i++) {
            loginWithCookieJson(objectMapper.writeValueAsString(request), clientAddress)
                    .andExpect(status().isUnauthorized());
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
                .andExpect(jsonPath("$.sub").value(validRequest.email()))
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

        mockMvc.perform(get("/api/auth/me").cookie(authCookie))
                .andExpect(status().isUnauthorized());
    }


    private org.springframework.test.web.servlet.ResultActions loginWithCookieJson(
            String json, String remoteAddress) throws Exception {
        CsrfValues csrf = obtainCsrf();
        var request = post("/api/auth/login-cookie")
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
        var response = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        String setCookie = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(header -> header.startsWith("XSRF-TOKEN="))
                .findFirst().orElseThrow();
        String cookieToken = setCookie.substring("XSRF-TOKEN=".length()).split(";", 2)[0];
        return new CsrfValues(new Cookie("XSRF-TOKEN", cookieToken), cookieToken);
    }

    private Cookie loginWithCookie(CsrfValues csrf, boolean rememberMe) throws Exception {
        var response = mockMvc.perform(post("/api/auth/login-cookie")
                        .param("rememberMe", Boolean.toString(rememberMe))
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(validRequest.email(), validRequest.password()))))
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
                .andExpect(jsonPath("$.sub").value(validRequest.email()));
    }

    @Test
    @DisplayName("Should issue a session authentication cookie when remember me is disabled")
    void shouldIssueSessionCookieWithoutRememberMe() throws Exception {
        createActiveBreeder();
        Cookie authCookie = loginWithCookie(obtainCsrf(), false);

        mockMvc.perform(get("/api/notifications/unread-count").cookie(authCookie))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject cookie login without a CSRF token")
    void shouldRejectCookieLoginWithoutCsrf() throws Exception {
        createActiveBreeder();

        mockMvc.perform(post("/api/auth/login-cookie")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(validRequest.email(), validRequest.password()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should block state-changing cookie requests without a CSRF token")
    void shouldRejectCookieAuthenticatedChangesWithoutCsrf() throws Exception {
        createActiveBreeder();
        Cookie authCookie = loginWithCookie(obtainCsrf(), false);

        mockMvc.perform(put("/api/notifications/123456789/read").cookie(authCookie))
                .andExpect(status().isForbidden());
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
    @DisplayName("Should not register the removed legacy login endpoint")
    void shouldNotRegisterLegacyLoginEndpoint() {
        boolean legacyLoginRegistered = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                .anyMatch(mapping -> mapping.getPatternValues().contains("/api/auth/login")
                        && (mapping.getMethodsCondition().getMethods().isEmpty()
                        || mapping.getMethodsCondition().getMethods().contains(RequestMethod.POST)));

        assertFalse(legacyLoginRegistered);
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

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(authCookie, csrf.cookie()))
                .andExpect(status().isForbidden());
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