package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.BreederResponseDto;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
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

    // Pobieranie kont oczekujących na akceptację
    public List<BreederResponseDto> getPendingAccounts() {
        return breederRepository.findByStatus(AccountStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Pobieranie zaakceptowanych, aktywnych kont
    public List<BreederResponseDto> getAllRegisteredAccounts() {
        return breederRepository.findByStatusIn(List.of(AccountStatus.ACTIVE, AccountStatus.BLOCKED))
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveAccount(Long id) {
        Breeder breeder = breederRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));

        if (breeder.getStatus() != AccountStatus.PENDING) {
            throw new RuntimeException("Konto nie ma statusu oczekującego na akceptację.");
        }

        breeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.save(breeder);
    }

    @Transactional
    public void rejectAccount(Long id) {
        Breeder breeder = breederRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));

        if (breeder.getStatus() != AccountStatus.PENDING) {
            throw new RuntimeException("Można usunąć fizycznie tylko konta o statusie oczekującym.");
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
            throw new RuntimeException("Tylko aktywne konta mogą zostać zablokowane.");
        }

        breeder.setStatus(AccountStatus.BLOCKED);
        breederRepository.save(breeder);
    }

    @Transactional
    public void unblockAccount(Long id) {
        Breeder breeder = breederRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));

        if (breeder.getStatus() != AccountStatus.BLOCKED) {
            throw new RuntimeException("Tylko zablokowane konta mogą zostać odblokowane.");
        }

        breeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.save(breeder);
    }

    @Transactional
    public void changeRole(Long id, String newRoleStr) {
        Breeder breeder = breederRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + id));

        if (breeder.getRole() == Role.ADMINISTRATOR) {
            throw new IllegalStateException("Nie możesz zmieniać uprawnień innym administratorom.");
        }

        try {
            Role newRole = Role.valueOf(newRoleStr.toUpperCase());
            if (newRole != Role.BREEDER && newRole != Role.MODERATOR) {
                throw new IllegalArgumentException("Przekazano nieprawidłową rolę.");
            }
            breeder.setRole(newRole);
            breederRepository.save(breeder);
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
                breeder.getSectionId(),
                breeder.getStatus(),
                breeder.getRole().name(),
                breeder.getCreatedAt()
        );
    }
}