package com.pzhgp.backend.controller;

import tools.jackson.databind.json.JsonMapper;
import com.pzhgp.backend.dto.LoginRequest;
import com.pzhgp.backend.dto.RegistrationRequest;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private AuthRateLimiter authRateLimiter;

    @Autowired
    private JsonMapper objectMapper;

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
    @DisplayName("Should return a 400 Bad Request status code if the input data is missing or incorrect")
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
    @DisplayName("Should return 409 Conflict when attempting to register with an existing email address")
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
    @DisplayName("Should return 409 Conflict when attempting to register with an existing phone number")
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
    @DisplayName("Should return 429 after exceeding the registration attempt limit")
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
    @DisplayName("Should return 429 after exceeding the login attempt limit")
    void shouldReturnTooManyRequestsAfterLoginLimit() throws Exception {
        String clientAddress = "192.0.2.10";
        LoginRequest request = new LoginRequest("unknown@test.pl", "WrongPassword123!");

        for (int i = 0; i < 30; i++) {
            mockMvc.perform(post("/api/auth/login").with(httpRequest -> {
                                httpRequest.setRemoteAddr(clientAddress);
                                return httpRequest;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login").with(httpRequest -> {
                            httpRequest.setRemoteAddr(clientAddress);
                            return httpRequest;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Zbyt wiele prób logowania. Spróbuj ponownie później."));
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
    @DisplayName("Should handle the registration correctly and return status code 201")
    void shouldRegisterUserSuccessfullyIntegration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized status when attempting to log in with missing email in DB")
    void shouldReturnUnauthorizedOnInvalidLogin() throws Exception {
        LoginRequest invalidLogin = new LoginRequest("doesntexist@test.pl", "BadPassword123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized status when existing user provides wrong password")
    void shouldReturnUnauthorizedOnWrongPasswordForExistingUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        Breeder savedBreeder = breederRepository.findByEmail(validRequest.email()).get();
        savedBreeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.save(savedBreeder);

        LoginRequest wrongPassLogin = new LoginRequest(validRequest.email(), "ZleHaslo999!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Nieprawidłowy adres e-mail lub hasło."));
    }

    @Test
    @DisplayName("Should return 403 Forbidden status when logging into PENDING account")
    void shouldReturnForbiddenWhenLoggingInWithPendingAccount() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(validRequest.email(), validRequest.password());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Twoje konto oczekuje jeszcze na akceptację administratora."));
    }

    @Test
    @DisplayName("Should successfully log in the active user, return a JWT token and pass Security Filter to reach protected endpoint")
    void shouldLoginSuccessfullyAndReturnJwtIntegration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        Breeder savedBreeder = breederRepository.findByEmail(validRequest.email()).get();
        savedBreeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.save(savedBreeder);

        LoginRequest loginRequest = new LoginRequest(validRequest.email(), validRequest.password());
        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String jwtToken = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(jwtToken.isEmpty(), "Zwrócony token JWT nie powinien być pusty");

        mockMvc.perform(get("/api/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should login successfully with differently cased email address")
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

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson)).andExpect(status().isOk());
    }
}