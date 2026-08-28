package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.BreederResponseDto;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.NotificationType;
import com.pzhgp.backend.entity.Role;
import com.pzhgp.backend.repository.BreederRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final BreederRepository breederRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<BreederResponseDto> getPendingAccounts() {
        return breederRepository.findByStatus(AccountStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BreederResponseDto> getAllRegisteredAccounts() {
        return breederRepository.findByStatusIn(List.of(AccountStatus.ACTIVE, AccountStatus.BLOCKED))
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveAccount(Long id, String adminEmail) {
        Breeder breeder = breederRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));

        Breeder admin = breederRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono administratora."));

        if (breeder.getStatus() != AccountStatus.PENDING) {
            throw new IllegalStateException("Konto nie ma statusu oczekującego na akceptację.");
        }

        breeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.save(breeder);

        String adminFullName = admin.getName() + " " + admin.getSurname();
        notificationService.createNotification(
                breeder.getId(),
                "Administrator " + adminFullName + " zatwierdził twoje konto. Witaj w systemie PZHGP Żagań!",
                "/",
                NotificationType.ACCOUNT_APPROVED
        );
    }

    @Transactional
    public void rejectAccount(Long id) {
        Breeder breeder = breederRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));

        if (breeder.getStatus() != AccountStatus.PENDING) {
            throw new IllegalStateException("Można usunąć fizycznie tylko konta o statusie oczekującym.");
        }

        breederRepository.delete(breeder);
    }

    @Transactional
    public void blockAccount(Long id) {
        Breeder breeder = breederRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));

        if (breeder.getRole() == Role.ADMINISTRATOR) {
            throw new IllegalStateException("Odmowa dostępu: Nie możesz zablokować konta innego administratora.");
        }

        if (breeder.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Tylko aktywne konta mogą zostać zablokowane.");
        }

        breeder.setStatus(AccountStatus.BLOCKED);
        breederRepository.save(breeder);
    }

    @Transactional
    public void unblockAccount(Long id) {
        Breeder breeder = breederRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));

        if (breeder.getStatus() != AccountStatus.BLOCKED) {
            throw new IllegalStateException("Tylko zablokowane konta mogą zostać odblokowane.");
        }

        breeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.save(breeder);
    }

    @Transactional
    public void changeRole(Long id, String newRoleStr, String adminEmail) {
        Breeder breeder = breederRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));

        Breeder admin = breederRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono administratora."));

        if (breeder.getRole() == Role.ADMINISTRATOR) {
            throw new IllegalStateException("Nie możesz zmieniać uprawnień innym administratorom.");
        }

        if (newRoleStr == null) {
            throw new IllegalArgumentException("Przekazano nieprawidłową rolę.");
        }

        try {
            Role newRole = Role.valueOf(newRoleStr.toUpperCase());
            if (newRole != Role.BREEDER && newRole != Role.MODERATOR) {
                throw new IllegalArgumentException("Przekazano nieprawidłową rolę.");
            }
            breeder.setRole(newRole);
            breederRepository.save(breeder);

            String roleNamePL = (newRole == Role.MODERATOR) ? "Moderator" : "Hodowca";
            String adminFullName = admin.getName() + " " + admin.getSurname();

            notificationService.createNotification(
                    breeder.getId(),
                    "Administrator " + adminFullName + " zmienił twoją rolę na: " + roleNamePL,
                    "/settings",
                    NotificationType.ROLE_CHANGED
            );

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Przekazano nieprawidłową rolę.");
        }
    }

    private BreederResponseDto mapToDto(Breeder breeder) {
        return new BreederResponseDto(
                breeder.getId(),
                breeder.getName(),
                breeder.getSurname(),
                breeder.getEmail(),
                breeder.getPhoneNumber(),
                breeder.getDateOfBirth(),
                breeder.getPostalCode(),
                breeder.getCity(),
                breeder.getStreet(),
                breeder.getHouseNumber(),
                breeder.getSection().getId().intValue(),
                breeder.getSection().getName(),
                breeder.getStatus(),
                breeder.getRole().name(),
                breeder.getCreatedAt()
        );
    }
}