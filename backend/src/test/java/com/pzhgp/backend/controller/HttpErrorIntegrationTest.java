package com.pzhgp.backend.controller;

import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.Role;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.SectionRepository;
import com.pzhgp.backend.service.JwtService;
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

import static com.pzhgp.backend.support.CookieAuthTestSupport.cookieAuth;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("HTTP Error Handling Integration Tests")
class HttpErrorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        Breeder breeder = new Breeder();
        breeder.setEmail("http-errors@test.pl");
        breeder.setRole(Role.BREEDER);
        breeder.setStatus(AccountStatus.ACTIVE);
        breeder.setName("Jan");
        breeder.setSurname("Kowalski");
        breeder.setPhoneNumber("123456789");
        breeder.setPasswordHash("hashed");
        breeder.setSection(sectionRepository.findAll().getFirst());

        token = jwtService.generateToken(breederRepository.save(breeder));
    }

    @Test
    @DisplayName("Should return 404 instead of 500 for an unknown address")
    void shouldReturnNotFoundForUnknownAddress() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").with(cookieAuth(token)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Nie znaleziono zasobu pod podanym adresem."));
    }

    @Test
    @DisplayName("Should return error messages as plain text even when the client asks for JSON")
    void shouldReturnPlainTextErrorsForJsonClients() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").with(cookieAuth(token)).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("Nie znaleziono zasobu pod podanym adresem."));

        mockMvc.perform(get("/api/forum/threads/999999").with(cookieAuth(token)).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("text/plain;charset=UTF-8"));
    }

    @Test
    @DisplayName("Should return 405 with the Allow header for an unsupported HTTP method")
    void shouldReturnMethodNotAllowedForUnsupportedMethod() throws Exception {
        mockMvc.perform(delete("/api/sections"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")))
                .andExpect(content().string("Metoda DELETE nie jest obsługiwana pod tym adresem."));
    }

    @Test
    @DisplayName("Should return 415 for an unsupported request content type")
    void shouldReturnUnsupportedMediaTypeForWrongContentType() throws Exception {
        mockMvc.perform(post("/api/forum/threads")
                        .with(cookieAuth(token))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Nowy wątek"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().string("Nieobsługiwany format danych żądania."));
    }
}
