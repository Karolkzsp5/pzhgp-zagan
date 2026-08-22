package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.BreederResponseDto;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.Role;
import com.pzhgp.backend.entity.Section;
import com.pzhgp.backend.repository.BreederRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private BreederRepository breederRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdminService adminService;

    private Breeder pendingBreeder;
    private Breeder activeBreeder;
    private Breeder adminBreeder;

    @BeforeEach
    void setUp() {
        pendingBreeder = createBreeder(1L, "Jan", "Kowalski", AccountStatus.PENDING, Role.BREEDER);
        activeBreeder = createBreeder(2L, "Adam", "Nowak", AccountStatus.ACTIVE, Role.BREEDER);
        adminBreeder = createBreeder(3L, "Prezes", "Oddzialu", AccountStatus.ACTIVE, Role.ADMINISTRATOR);
    }

    private Breeder createBreeder(Long id, String name, String surname, AccountStatus status, Role role) {
        Breeder breeder = new Breeder();
        breeder.setId(id);
        breeder.setName(name);
        breeder.setSurname(surname);
        breeder.setEmail(name.toLowerCase() + "@test.pl");
        breeder.setPhoneNumber("111222333");
        breeder.setDateOfBirth(LocalDate.of(1980, 1, 1));
        Section section = new Section(1L, "Żagań", 1);
        breeder.setSection(section);
        breeder.setStatus(status);
        breeder.setRole(role);
        breeder.setCreatedAt(LocalDateTime.now());
        return breeder;
    }

    @Test
    @DisplayName("Should return only PENDING accounts")
    void shouldReturnPendingAccounts() {
        when(breederRepository.findByStatus(AccountStatus.PENDING)).thenReturn(List.of(pendingBreeder));

        List<BreederResponseDto> result = adminService.getPendingAccounts();

        assertEquals(1, result.size());
        assertEquals(AccountStatus.PENDING, result.get(0).status());
        verify(breederRepository, times(1)).findByStatus(AccountStatus.PENDING);
    }

    @Test
    @DisplayName("Should return only ACTIVE and BLOCKED accounts (Registered)")
    void shouldReturnRegisteredAccounts() {
        Breeder blockedBreeder = createBreeder(4L, "Zly", "Hodowca", AccountStatus.BLOCKED, Role.BREEDER);
        when(breederRepository.findByStatusIn(List.of(AccountStatus.ACTIVE, AccountStatus.BLOCKED)))
                .thenReturn(List.of(activeBreeder, blockedBreeder));

        List<BreederResponseDto> result = adminService.getAllRegisteredAccounts();

        assertEquals(2, result.size());
        verify(breederRepository, times(1)).findByStatusIn(List.of(AccountStatus.ACTIVE, AccountStatus.BLOCKED));
    }

    @Test
    @DisplayName("Should approve PENDING account successfully")
    void shouldApproveAccount() {
        when(breederRepository.findById(1L)).thenReturn(Optional.of(pendingBreeder));

        assertDoesNotThrow(() -> adminService.approveAccount(1L));

        assertEquals(AccountStatus.ACTIVE, pendingBreeder.getStatus());
        verify(breederRepository, times(1)).save(pendingBreeder);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when approving non-PENDING account")
    void shouldThrowExceptionWhenApprovingActiveAccount() {
        when(breederRepository.findById(2L)).thenReturn(Optional.of(activeBreeder));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> adminService.approveAccount(2L));

        assertEquals("Konto nie ma statusu oczekującego na akceptację.", ex.getMessage());
        verify(breederRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when account does not exist")
    void shouldThrowEntityNotFoundWhenAccountMissing() {
        when(breederRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> adminService.approveAccount(99L));

        assertEquals("Nie znaleziono hodowcy o ID: 99", ex.getMessage());
    }

    @Test
    @DisplayName("Should delete account when rejecting PENDING account")
    void shouldRejectAccount() {
        when(breederRepository.findById(1L)).thenReturn(Optional.of(pendingBreeder));

        assertDoesNotThrow(() -> adminService.rejectAccount(1L));

        verify(breederRepository, times(1)).delete(pendingBreeder);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when rejecting non-PENDING account")
    void shouldThrowExceptionWhenRejectingActiveAccount() {
        when(breederRepository.findById(2L)).thenReturn(Optional.of(activeBreeder));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> adminService.rejectAccount(2L));

        assertEquals("Można usunąć fizycznie tylko konta o statusie oczekującym.", ex.getMessage());
        verify(breederRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should successfully block an ACTIVE account")
    void shouldBlockActiveAccount() {
        when(breederRepository.findById(2L)).thenReturn(Optional.of(activeBreeder));

        assertDoesNotThrow(() -> adminService.blockAccount(2L));

        assertEquals(AccountStatus.BLOCKED, activeBreeder.getStatus());
        verify(breederRepository, times(1)).save(activeBreeder);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when trying to block another ADMINISTRATOR")
    void shouldPreventBlockingAdministrator() {
        when(breederRepository.findById(3L)).thenReturn(Optional.of(adminBreeder));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> adminService.blockAccount(3L));

        assertEquals("Odmowa dostępu: Nie możesz zablokować konta innego administratora.", ex.getMessage());
        verify(breederRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when trying to block non-ACTIVE account")
    void shouldThrowExceptionWhenBlockingNonActiveAccount() {
        when(breederRepository.findById(1L)).thenReturn(Optional.of(pendingBreeder));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> adminService.blockAccount(1L));

        assertEquals("Tylko aktywne konta mogą zostać zablokowane.", ex.getMessage());
        verify(breederRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully unblock a BLOCKED account")
    void shouldUnblockBlockedAccount() {
        Breeder blockedBreeder = createBreeder(4L, "Zly", "Hodowca", AccountStatus.BLOCKED, Role.BREEDER);
        when(breederRepository.findById(4L)).thenReturn(Optional.of(blockedBreeder));

        assertDoesNotThrow(() -> adminService.unblockAccount(4L));

        assertEquals(AccountStatus.ACTIVE, blockedBreeder.getStatus());
        verify(breederRepository, times(1)).save(blockedBreeder);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when trying to unblock non-BLOCKED account")
    void shouldThrowExceptionWhenUnblockingNonBlockedAccount() {
        when(breederRepository.findById(2L)).thenReturn(Optional.of(activeBreeder));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> adminService.unblockAccount(2L));

        assertEquals("Tylko zablokowane konta mogą zostać odblokowane.", ex.getMessage());
        verify(breederRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully change role to MODERATOR")
    void shouldChangeRoleSuccessfully() {
        when(breederRepository.findById(2L)).thenReturn(Optional.of(activeBreeder));

        assertDoesNotThrow(() -> adminService.changeRole(2L, "MODERATOR"));

        assertEquals(Role.MODERATOR, activeBreeder.getRole());
        verify(breederRepository, times(1)).save(activeBreeder);
    }

    @Test
    @DisplayName("Should successfully change role to BREEDER")
    void shouldChangeRoleToBreederSuccessfully() {
        activeBreeder.setRole(Role.MODERATOR);
        when(breederRepository.findById(2L)).thenReturn(Optional.of(activeBreeder));

        assertDoesNotThrow(() -> adminService.changeRole(2L, "BREEDER"));

        assertEquals(Role.BREEDER, activeBreeder.getRole());
        verify(breederRepository, times(1)).save(activeBreeder);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when changing to invalid role")
    void shouldThrowExceptionOnInvalidRole() {
        when(breederRepository.findById(2L)).thenReturn(Optional.of(activeBreeder));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> adminService.changeRole(2L, "SUPERMAN"));

        assertEquals("Przekazano nieprawidłową rolę.", ex.getMessage());
        verify(breederRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when role is null")
    void shouldThrowExceptionWhenRoleIsNull() {
        when(breederRepository.findById(2L)).thenReturn(Optional.of(activeBreeder));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adminService.changeRole(2L, null));

        assertEquals("Przekazano nieprawidłową rolę.", ex.getMessage());
        verify(breederRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when trying to change role of ADMINISTRATOR")
    void shouldPreventChangingAdminRole() {
        when(breederRepository.findById(3L)).thenReturn(Optional.of(adminBreeder));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> adminService.changeRole(3L, "BREEDER"));

        assertEquals("Nie możesz zmieniać uprawnień innym administratorom.", ex.getMessage());
        verify(breederRepository, never()).save(any());
    }
}