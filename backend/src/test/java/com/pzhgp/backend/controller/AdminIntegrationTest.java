package com.pzhgp.backend.controller;

import tools.jackson.databind.json.JsonMapper;
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
@DisplayName("Admin Panel Integration Tests")
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

    @Autowired
    private JsonMapper objectMapper;

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

        Breeder admin = createRealUser("admin@test.pl", "111111111", Role.ADMINISTRATOR, AccountStatus.ACTIVE, section);
        Breeder breeder = createRealUser("breeder@test.pl", "222222222", Role.BREEDER, AccountStatus.ACTIVE, section);
        Breeder moderator = createRealUser("moderator@test.pl", "333333333", Role.MODERATOR, AccountStatus.ACTIVE, section);

        pendingBreeder = createRealUser("pending@test.pl", "444444444", Role.BREEDER, AccountStatus.PENDING, section);
        activeBreeder = createRealUser("active@test.pl", "555555555", Role.BREEDER, AccountStatus.ACTIVE, section);
        blockedBreeder = createRealUser("blocked@test.pl", "666666666", Role.BREEDER, AccountStatus.BLOCKED, section);
        anotherAdmin = createRealUser("admin2@test.pl", "777777777", Role.ADMINISTRATOR, AccountStatus.ACTIVE, section);

        adminToken = "Bearer " + jwtService.generateToken(admin);
        breederToken = "Bearer " + jwtService.generateToken(breeder);
        moderatorToken = "Bearer " + jwtService.generateToken(moderator);
    }

    private Breeder createRealUser(String email, String phoneNumber, Role role, AccountStatus status, Section section) {
        Breeder breeder = new Breeder();
        breeder.setEmail(email);
        breeder.setRole(role);
        breeder.setStatus(status);
        breeder.setName("Test");
        breeder.setSurname("User");
        breeder.setPhoneNumber(phoneNumber);
        breeder.setPasswordHash("hashed");
        breeder.setSection(section);
        return breederRepository.save(breeder);
    }

    @Test
    @DisplayName("Should allow an administrator to retrieve pending and registered accounts")
    void shouldAllowAccessForAdministratorGetEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/pending").header(HttpHeaders.AUTHORIZATION, adminToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("pending@test.pl"));

        mockMvc.perform(get("/api/admin/registered").header(HttpHeaders.AUTHORIZATION, adminToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @DisplayName("Should return 401 when an unauthenticated user accesses administrator endpoints")
    void shouldDenyAccessForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/admin/pending")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 403 when a breeder accesses administrator endpoints")
    void shouldDenyAccessForBreeder() throws Exception {
        mockMvc.perform(get("/api/admin/pending").header(HttpHeaders.AUTHORIZATION, breederToken)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/approve/" + pendingBreeder.getId()).header(HttpHeaders.AUTHORIZATION, breederToken)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 when a moderator accesses administrator endpoints")
    void shouldDenyAccessForModerator() throws Exception {
        mockMvc.perform(get("/api/admin/registered").header(HttpHeaders.AUTHORIZATION, moderatorToken)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/block/" + activeBreeder.getId()).header(HttpHeaders.AUTHORIZATION, moderatorToken)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should approve a pending account and notify the breeder")
    void shouldApproveAccount() throws Exception {
        long initialNotificationCount = notificationRepository.count();

        mockMvc.perform(put("/api/admin/approve/" + pendingBreeder.getId()).header(HttpHeaders.AUTHORIZATION, adminToken)).andExpect(status().isOk());

        Breeder updated = breederRepository.findById(pendingBreeder.getId()).get();
        assertEquals(AccountStatus.ACTIVE, updated.getStatus());

        assertEquals(initialNotificationCount + 1, notificationRepository.count());
        List<Notification> notifications = notificationRepository.findAll();
        Notification approvalNotification = notifications.stream().filter(n -> n.getType() == NotificationType.ACCOUNT_APPROVED).findFirst().orElseThrow();

        assertEquals(pendingBreeder.getId(), approvalNotification.getRecipient().getId());
    }

    @Test
    @DisplayName("Should return 404 when approving a non-existent account")
    void shouldReturnNotFoundWhenEntityDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/admin/approve/99999")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reject a pending account and delete it from the database")
    void shouldRejectAccount() throws Exception {
        mockMvc.perform(delete("/api/admin/reject/" + pendingBreeder.getId()).header(HttpHeaders.AUTHORIZATION, adminToken)).andExpect(status().isOk());
        assertTrue(breederRepository.findById(pendingBreeder.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should block an active account")
    void shouldBlockAccount() throws Exception {
        mockMvc.perform(put("/api/admin/block/" + activeBreeder.getId()).header(HttpHeaders.AUTHORIZATION, adminToken)).andExpect(status().isOk());

        Breeder updated = breederRepository.findById(activeBreeder.getId()).get();
        assertEquals(AccountStatus.BLOCKED, updated.getStatus());
    }

    @Test
    @DisplayName("Should return 403 when an administrator attempts to block another administrator")
    void shouldReturnForbiddenWhenActionIsDenied() throws Exception {
        mockMvc.perform(put("/api/admin/block/" + anotherAdmin.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should unblock a blocked account")
    void shouldUnblockAccount() throws Exception {
        mockMvc.perform(put("/api/admin/unblock/" + blockedBreeder.getId()).header(HttpHeaders.AUTHORIZATION, adminToken)).andExpect(status().isOk());

        Breeder updated = breederRepository.findById(blockedBreeder.getId()).get();
        assertEquals(AccountStatus.ACTIVE, updated.getStatus());
    }

    @Test
    @DisplayName("Should change a breeder's role and create a notification")
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
        Notification roleChangeNotification = notifications.stream().filter(n -> n.getType() == NotificationType.ROLE_CHANGED).findFirst().orElseThrow();

        assertEquals(activeBreeder.getId(), roleChangeNotification.getRecipient().getId());
    }

    @Test
    @DisplayName("Should return 403 when a downgraded administrator uses an outdated JWT to change a role")
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
    @DisplayName("Should return 400 when role is missing from the request body")
    void shouldReturnBadRequestWhenRoleIsMissing() throws Exception {
        mockMvc.perform(put("/api/admin/" + activeBreeder.getId() + "/role")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when the requested role is invalid")
    void shouldReturnBadRequestOnInvalidRoleString() throws Exception {
        Map<String, String> requestBody = Map.of("role", "SUPERMAN");
        mockMvc.perform(put("/api/admin/" + activeBreeder.getId() + "/role")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when a role change contains malformed JSON")
    void shouldReturnBadRequestOnMalformedJson() throws Exception {
        String malformedJson = "{\"role\": \"MODERATOR\"";

        mockMvc.perform(put("/api/admin/" + activeBreeder.getId() + "/role")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when an account ID is not numeric")
    void shouldReturnBadRequestWhenIdIsInvalidType() throws Exception {
        mockMvc.perform(put("/api/admin/approve/abc").header(HttpHeaders.AUTHORIZATION, adminToken)).andExpect(status().isBadRequest());
    }
}