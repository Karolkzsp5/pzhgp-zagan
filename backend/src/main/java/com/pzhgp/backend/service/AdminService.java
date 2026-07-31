package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.BreederResponseDto;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.repository.BreederRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final BreederRepository breederRepository;

    public List<BreederResponseDto> getPendingAccounts() {
        return breederRepository.findByStatus(AccountStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveAccount(Long id) {
        Breeder breeder = breederRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono hodowcy o ID: " + id));

        if (breeder.getStatus() != AccountStatus.PENDING) {
            throw new RuntimeException("Konto nie ma statusu oczekującego na akceptację.");
        }

        breeder.setStatus(AccountStatus.ACTIVE);
        breederRepository.save(breeder);
    }

    @Transactional
    public void rejectAccount(Long id) {
        Breeder breeder = breederRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono hodowcy o ID: " + id));

        breederRepository.delete(breeder);
    }

    private BreederResponseDto mapToDto(Breeder breeder) {
        return new BreederResponseDto(
                breeder.getId(),
                breeder.getName(),
                breeder.getSurname(),
                breeder.getEmail(),
                breeder.getPhoneNumber(),
                breeder.getDateOfBirth(),
                breeder.getCity(),
                breeder.getSectionId(),
                breeder.getStatus(),
                breeder.getCreatedAt()
        );
    }
}