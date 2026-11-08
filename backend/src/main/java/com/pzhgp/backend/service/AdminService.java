package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.BreederDto;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.NotificationType;
import com.pzhgp.backend.entity.Role;
import com.pzhgp.backend.exception.ForbiddenOperationException;
import com.pzhgp.backend.exception.ResourceConflictException;
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
    public List<BreederDto> getPendingAccounts() {
        return breederRepository.findByStatus(AccountStatus.PENDING).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BreederDto> getAllRegisteredAccounts() {
        return breederRepository.findByStatusIn(List.of(AccountStatus.ACTIVE, AccountStatus.BLOCKED)).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public void approveAccount(Long id, String adminEmail) {
        Breeder breeder = breederRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));
        Breeder admin = breederRepository.requireByEmail(adminEmail);

        if (breeder.getStatus() != AccountStatus.PENDING) {
            throw new ResourceConflictException("Konto nie ma statusu oczekującego na akceptację.");
        }

        breeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.save(breeder);

        notificationService.createNotification(
                breeder.getId(),
                "Administrator " + admin.getFullName() + " zatwierdził Twoje konto. Witaj w systemie PZHGP Żagań!",
                "/",
                NotificationType.ACCOUNT_APPROVED
        );
    }

    @Transactional
    public void rejectAccount(Long id) {
        Breeder breeder = breederRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));

        if (breeder.getStatus() != AccountStatus.PENDING) {
            throw new ResourceConflictException("Można usunąć fizycznie tylko konta o statusie oczekującym.");
        }

        breederRepository.delete(breeder);
    }

    @Transactional
    public void blockAccount(Long id) {
        Breeder breeder = breederRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));

        if (breeder.getRole() == Role.ADMINISTRATOR) {
            throw new ForbiddenOperationException("Odmowa dostępu: Nie możesz zablokować konta innego administratora.");
        }

        if (breeder.getStatus() != AccountStatus.ACTIVE) {
            throw new ResourceConflictException("Tylko aktywne konta mogą zostać zablokowane.");
        }

        breeder.setStatus(AccountStatus.BLOCKED);
        breederRepository.save(breeder);
    }

    @Transactional
    public void unblockAccount(Long id) {
        Breeder breeder = breederRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));

        if (breeder.getStatus() != AccountStatus.BLOCKED) {
            throw new ResourceConflictException("Tylko zablokowane konta mogą zostać odblokowane.");
        }

        breeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.save(breeder);
    }

    @Transactional
    public void changeRole(Long id, Role newRole, String adminEmail) {
        Breeder breeder = breederRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));
        Breeder admin = breederRepository.requireByEmail(adminEmail);

        if (breeder.getRole() == Role.ADMINISTRATOR) {
            throw new ForbiddenOperationException("Nie możesz zmieniać uprawnień innym administratorom.");
        }

        // Administratora można nadać tylko bezpośrednio w bazie danych.
        if (newRole != Role.BREEDER && newRole != Role.MODERATOR) {
            throw new IllegalArgumentException("Przekazano nieprawidłową rolę.");
        }

        breeder.setRole(newRole);
        breederRepository.save(breeder);

        String roleNamePL = (newRole == Role.MODERATOR) ? "Moderator" : "Hodowca";

        notificationService.createNotification(
                breeder.getId(),
                "Administrator " + admin.getFullName() + " zmienił Twoją rolę na: " + roleNamePL + ".",
                "/settings",
                NotificationType.ROLE_CHANGED
        );
    }

    private BreederDto mapToDto(Breeder breeder) {
        return new BreederDto(
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
                breeder.getSection().getId(),
                breeder.getSection().getName(),
                breeder.getStatus(),
                breeder.getRole(),
                breeder.getCreatedAt()
        );
    }
}