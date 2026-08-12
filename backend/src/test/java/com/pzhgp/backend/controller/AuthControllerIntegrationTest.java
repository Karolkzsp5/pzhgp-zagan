package com.pzhgp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pzhgp.backend.dto.LoginRequest;
import com.pzhgp.backend.dto.RegistrationRequest;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.repository.BreederRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BreederRepository breederRepository;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private RegistrationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegistrationRequest(
                "Jan", "Kowalski", "1995-05-15", "68-100", "Żagań",
                "Słowackiego", "10", 1, "integration.test@pzhgp.pl", "444444444", "SecurePass1!"
        );
    }

    @Test
    @DisplayName("Powinien zwrócić status 400 Bad Request przy brakujących lub błędnych danych wejściowych")
    void shouldReturnBadRequestOnInvalidInputData() throws Exception {
        RegistrationRequest invalidRequest = new RegistrationRequest(
                "", "", "", "", "", "", "", 0, "niepoprawny-email", "", ""
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Powinien zwrócić status 400 Bad Request przy próbie rejestracji z istniejącym mailem")
    void shouldReturnBadRequestWhenEmailIsDuplicatedIntegration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        RegistrationRequest duplicateRequest = new RegistrationRequest(
                "Andrzej", "Nowak", "1990-01-01", "68-100", "Żagań",
                "Długa", "5", 1, "integration.test@pzhgp.pl", "111222333", "OtherPass1!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Użytkownik z podanym adresem e-mail już istnieje."));
    }

    @Test
    @DisplayName("INTEGRACJA: Powinien zwrócić status 400 Bad Request przy próbie rejestracji z istniejącym numerem telefonu")
    void shouldReturnBadRequestWhenPhoneNumberIsDuplicatedIntegration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        RegistrationRequest duplicatePhoneRequest = new RegistrationRequest(
                "Andrzej", "Nowak", "1990-01-01", "68-100", "Żagań",
                "Długa", "5", 1, "inny.mail@pzhgp.pl", "444444444", "InneHaslo1!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicatePhoneRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Użytkownik z podanym numerem telefonu już istnieje."));
    }

    @Test
    @DisplayName("Powinien obsłużyć poprawną rejestrację i zwrócić status 201")
    void shouldRegisterUserSuccessfullyIntegration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Powinien zwrócić status 401 Unauthorized przy próbie logowania z błędnymi danymi")
    void shouldReturnUnauthorizedOnInvalidLogin() throws Exception {
        LoginRequest invalidLogin = new LoginRequest("doesntexist@test.pl", "BadPassword123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Powinien pomyślnie zalogować aktywnego użytkownika i zwrócić token JWT")
    void shouldLoginSuccessfullyAndReturnJwtIntegration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        Breeder savedBreeder = breederRepository.findByEmail(validRequest.email())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika po rejestracji"));
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
    }
}