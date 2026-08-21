package com.pzhgp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.Role;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.service.AdminService;
import com.pzhgp.backend.service.JwtService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private BreederRepository breederRepository;

    @Autowired
    private JwtService jwtService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;
    private String breederToken;
    private String moderatorToken;

    @BeforeEach
    void setUp() {
        Breeder admin = createMockUser(Role.ADMINISTRATOR, "admin@test.pl");
        Breeder breeder = createMockUser(Role.BREEDER, "breeder@test.pl");
        Breeder moderator = createMockUser(Role.MODERATOR, "moderator@test.pl");

        when(breederRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(breederRepository.findByEmail(breeder.getEmail())).thenReturn(Optional.of(breeder));
        when(breederRepository.findByEmail(moderator.getEmail())).thenReturn(Optional.of(moderator));

        adminToken = "Bearer " + jwtService.generateToken(admin);
        breederToken = "Bearer " + jwtService.generateToken(breeder);
        moderatorToken = "Bearer " + jwtService.generateToken(moderator);
    }

    private Breeder createMockUser(Role role, String email) {
        Breeder breeder = new Breeder();
        breeder.setEmail(email);
        breeder.setRole(role);
        breeder.setStatus(AccountStatus.ACTIVE);
        return breeder;
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
                        .header("Authorization", breederToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/approve/1")
                        .header("Authorization", breederToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when MODERATOR tries to access admin endpoints")
    void shouldDenyAccessForModerator() throws Exception {
        mockMvc.perform(get("/api/admin/registered")
                        .header("Authorization", moderatorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/block/1")
                        .header("Authorization", moderatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 200 OK when ADMINISTRATOR accesses GET endpoints")
    void shouldAllowAccessForAdministratorGetEndpoints() throws Exception {
        when(adminService.getPendingAccounts()).thenReturn(Collections.emptyList());
        when(adminService.getAllRegisteredAccounts()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/pending").header("Authorization", adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/registered").header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow ADMINISTRATOR to approve account")
    void shouldApproveAccount() throws Exception {
        mockMvc.perform(put("/api/admin/approve/1").header("Authorization", adminToken))
                .andExpect(status().isOk());
        verify(adminService, times(1)).approveAccount(1L);
    }

    @Test
    @DisplayName("Should allow ADMINISTRATOR to reject account")
    void shouldRejectAccount() throws Exception {
        mockMvc.perform(delete("/api/admin/reject/1").header("Authorization", adminToken))
                .andExpect(status().isOk());
        verify(adminService, times(1)).rejectAccount(1L);
    }

    @Test
    @DisplayName("Should allow ADMINISTRATOR to block account")
    void shouldBlockAccount() throws Exception {
        mockMvc.perform(put("/api/admin/block/1").header("Authorization", adminToken))
                .andExpect(status().isOk());
        verify(adminService, times(1)).blockAccount(1L);
    }

    @Test
    @DisplayName("Should allow ADMINISTRATOR to unblock account")
    void shouldUnblockAccount() throws Exception {
        mockMvc.perform(put("/api/admin/unblock/1").header("Authorization", adminToken))
                .andExpect(status().isOk());
        verify(adminService, times(1)).unblockAccount(1L);
    }

    @Test
    @DisplayName("Should allow ADMINISTRATOR to change role")
    void shouldAllowAdministratorToChangeRole() throws Exception {
        Map<String, String> requestBody = Map.of("role", "MODERATOR");
        mockMvc.perform(put("/api/admin/1/role")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
        verify(adminService, times(1)).changeRole(1L, "MODERATOR");
    }

    @Test
    @DisplayName("Should return 400 Bad Request when role data is missing or empty in the request body")
    void shouldReturnBadRequestWhenRoleIsMissing() throws Exception {
        mockMvc.perform(put("/api/admin/1/role")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to perform action on non-existent account")
    void shouldReturnNotFoundWhenEntityDoesNotExist() throws Exception {
        doThrow(new EntityNotFoundException("Nie znaleziono hodowcy o ID: 99"))
                .when(adminService).approveAccount(99L);

        mockMvc.perform(put("/api/admin/approve/99")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when action is denied (e.g., blocking another admin)")
    void shouldReturnForbiddenWhenActionIsDenied() throws Exception {
        doThrow(new IllegalStateException("Odmowa dostępu: Nie możesz zablokować konta innego administratora."))
                .when(adminService).blockAccount(3L);

        mockMvc.perform(put("/api/admin/block/3")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when ID parameter in URL is not a number")
    void shouldReturnBadRequestWhenIdIsInvalidType() throws Exception {
        mockMvc.perform(put("/api/admin/approve/abc")
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request on malformed JSON payload")
    void shouldReturnBadRequestOnMalformedJson() throws Exception {
        String malformedJson = "{\"role\": \"MODERATOR\"";

        mockMvc.perform(put("/api/admin/1/role")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }
}