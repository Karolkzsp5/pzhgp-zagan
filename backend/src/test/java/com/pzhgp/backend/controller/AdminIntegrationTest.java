package com.pzhgp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.NotificationRepository;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;
    private String breederToken;
    private String moderatorToken;

    private Breeder pendingBreeder;
    private Breeder activeBreeder;
    private Breeder blockedBreeder;
    private Breeder anotherAdmin;

    @BeforeEach
    void setUp() {
        Section section = new Section(null, "Test", 1);
        sectionRepository.save(section);

        Breeder admin = createRealUser("admin@test.pl", Role.ADMINISTRATOR, AccountStatus.ACTIVE, section);
        Breeder breeder = createRealUser("breeder@test.pl", Role.BREEDER, AccountStatus.ACTIVE, section);
        Breeder moderator = createRealUser("moderator@test.pl", Role.MODERATOR, AccountStatus.ACTIVE, section);

        pendingBreeder = createRealUser("pending@test.pl", Role.BREEDER, AccountStatus.PENDING, section);
        activeBreeder = createRealUser("active@test.pl", Role.BREEDER, AccountStatus.ACTIVE, section);
        blockedBreeder = createRealUser("blocked@test.pl", Role.BREEDER, AccountStatus.BLOCKED, section);
        anotherAdmin = createRealUser("admin2@test.pl", Role.ADMINISTRATOR, AccountStatus.ACTIVE, section);

        adminToken = "Bearer " + jwtService.generateToken(admin);
        breederToken = "Bearer " + jwtService.generateToken(breeder);
        moderatorToken = "Bearer " + jwtService.generateToken(moderator);
    }

    private Breeder createRealUser(String email, Role role, AccountStatus status, Section section) {
        Breeder breeder = new Breeder();
        breeder.setEmail(email);
        breeder.setRole(role);
        breeder.setStatus(status);
        breeder.setName("Test");
        breeder.setSurname("User");
        breeder.setPhoneNumber(String.valueOf(System.nanoTime()).substring(0, 9));
        breeder.setPasswordHash("hashed");
        breeder.setSection(section);
        return breederRepository.save(breeder);
    }

    @Test
    @DisplayName("Should return 403 when unauthenticated user tries to access admin endpoints")
    void shouldDenyAccessForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/admin/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when BREEDER tries to access admin endpoints")
    void shouldDenyAccessForBreeder() throws Exception {
        mockMvc.perform(get("/api/admin/pending")
                        .header(HttpHeaders.AUTHORIZATION, breederToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/approve/" + pendingBreeder.getId())
                        .header(HttpHeaders.AUTHORIZATION, breederToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when MODERATOR tries to access admin endpoints")
    void shouldDenyAccessForModerator() throws Exception {
        mockMvc.perform(get("/api/admin/registered")
                        .header(HttpHeaders.AUTHORIZATION, moderatorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/block/" + activeBreeder.getId())
                        .header(HttpHeaders.AUTHORIZATION, moderatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 200 OK and correct JSON content when ADMINISTRATOR accesses GET endpoints")
    void shouldAllowAccessForAdministratorGetEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/pending").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("pending@test.pl"));

        mockMvc.perform(get("/api/admin/registered").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @DisplayName("Should allow ADMINISTRATOR to approve account, update real DB and create Notification")
    void shouldApproveAccount() throws Exception {
        long initialNotificationCount = notificationRepository.count();

        mockMvc.perform(put("/api/admin/approve/" + pendingBreeder.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        Breeder updated = breederRepository.findById(pendingBreeder.getId()).get();
        assertEquals(AccountStatus.ACTIVE, updated.getStatus());

        assertEquals(initialNotificationCount + 1, notificationRepository.count());
        List<Notification> notifications = notificationRepository.findAll();
        Notification approvalNotification = notifications.stream()
                .filter(n -> n.getType() == NotificationType.ACCOUNT_APPROVED)
                .findFirst().orElseThrow();

        assertEquals(pendingBreeder.getId(), approvalNotification.getRecipient().getId());
    }

    @Test
    @DisplayName("Should allow ADMINISTRATOR to reject account and remove from real DB")
    void shouldRejectAccount() throws Exception {
        mockMvc.perform(delete("/api/admin/reject/" + pendingBreeder.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        assertTrue(breederRepository.findById(pendingBreeder.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should allow ADMINISTRATOR to block account in real DB")
    void shouldBlockAccount() throws Exception {
        mockMvc.perform(put("/api/admin/block/" + activeBreeder.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        Breeder updated = breederRepository.findById(activeBreeder.getId()).get();
        assertEquals(AccountStatus.BLOCKED, updated.getStatus());
    }

    @Test
    @DisplayName("Should allow ADMINISTRATOR to unblock account in real DB")
    void shouldUnblockAccount() throws Exception {
        mockMvc.perform(put("/api/admin/unblock/" + blockedBreeder.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        Breeder updated = breederRepository.findById(blockedBreeder.getId()).get();
        assertEquals(AccountStatus.ACTIVE, updated.getStatus());
    }

    @Test
    @DisplayName("Should allow ADMINISTRATOR to change role in real DB and create Notification")
    void shouldAllowAdministratorToChangeRole() throws Exception {
        long initialNotificationCount = notificationRepository.count();
        Map<String, String> requestBody = Map.of("role", "MODERATOR");

        mockMvc.perform(put("/api/admin/" + activeBreeder.getId() + "/role")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());

        Breeder updated = breederRepository.findById(activeBreeder.getId()).get();
        assertEquals(Role.MODERATOR, updated.getRole());

        assertEquals(initialNotificationCount + 1, notificationRepository.count());
        List<Notification> notifications = notificationRepository.findAll();
        Notification roleChangeNotification = notifications.stream()
                .filter(n -> n.getType() == NotificationType.ROLE_CHANGED)
                .findFirst().orElseThrow();

        assertEquals(activeBreeder.getId(), roleChangeNotification.getRecipient().getId());
    }

    @Test
    @DisplayName("PUT /api/admin/{id}/role - Role changed to BREEDER in DB after ADMIN JWT issuance should return 403")
    void shouldDenyRoleChangeWhenAdminRoleDowngradedInDb() throws Exception {
        Breeder currentAdmin = breederRepository.findByEmail("admin@test.pl").get();
        currentAdmin.setRole(Role.BREEDER);
        breederRepository.save(currentAdmin);

        Map<String, String> requestBody = Map.of("role", "MODERATOR");

        mockMvc.perform(put("/api/admin/" + activeBreeder.getId() + "/role")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when role data is missing or empty in the request body")
    void shouldReturnBadRequestWhenRoleIsMissing() throws Exception {
        mockMvc.perform(put("/api/admin/" + activeBreeder.getId() + "/role")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when role is completely invalid (e.g., SUPERMAN)")
    void shouldReturnBadRequestOnInvalidRoleString() throws Exception {
        Map<String, String> requestBody = Map.of("role", "SUPERMAN");
        mockMvc.perform(put("/api/admin/" + activeBreeder.getId() + "/role")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to perform action on non-existent account")
    void shouldReturnNotFoundWhenEntityDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/admin/approve/99999")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when action is denied (e.g., blocking another admin)")
    void shouldReturnForbiddenWhenActionIsDenied() throws Exception {
        mockMvc.perform(put("/api/admin/block/" + anotherAdmin.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when ID parameter in URL is not a number")
    void shouldReturnBadRequestWhenIdIsInvalidType() throws Exception {
        mockMvc.perform(put("/api/admin/approve/abc")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request on malformed JSON payload")
    void shouldReturnBadRequestOnMalformedJson() throws Exception {
        String malformedJson = "{\"role\": \"MODERATOR\"";

        mockMvc.perform(put("/api/admin/" + activeBreeder.getId() + "/role")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }
}