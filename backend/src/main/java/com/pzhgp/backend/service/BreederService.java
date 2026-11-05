package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.CurrentUserDto;
import com.pzhgp.backend.dto.LoginRequest;
import com.pzhgp.backend.dto.RegistrationRequest;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.Section;
import com.pzhgp.backend.exception.ForbiddenOperationException;
import com.pzhgp.backend.exception.ResourceConflictException;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.SectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BreederService {

    private final BreederRepository breederRepository;
    private final SectionRepository sectionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Hash losowego hasła, liczony przy pierwszym logowaniu na nieistniejące konto.
    private volatile String dummyPasswordHash;

    public void registerNewBreeder(RegistrationRequest request) {
        if (breederRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResourceConflictException("Użytkownik z podanym adresem e-mail już istnieje.");
        }

        if (request.phoneNumber() != null && breederRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ResourceConflictException("Użytkownik z podanym numerem telefonu już istnieje.");
        }

        Section section = sectionRepository.findById(request.sectionId()).orElseThrow(() ->
                new EntityNotFoundException("Nie znaleziono sekcji o podanym ID: " + request.sectionId()));

        Breeder newBreeder = new Breeder();
        newBreeder.setName(request.name());
        newBreeder.setSurname(request.surname());
        newBreeder.setEmail(request.email());
        newBreeder.setPhoneNumber(request.phoneNumber());
        newBreeder.setDateOfBirth(request.dateOfBirth());
        newBreeder.setPostalCode(request.postalCode());
        newBreeder.setCity(request.city());
        newBreeder.setStreet(request.street());
        newBreeder.setHouseNumber(request.houseNumber());
        newBreeder.setSection(section);
        newBreeder.setPasswordHash(passwordEncoder.encode(request.password()));

        try {
            breederRepository.saveAndFlush(newBreeder);
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceConflictException("Użytkownik z podanym adresem e-mail lub numerem telefonu już istnieje.", ex);
        }
    }

    public String login(LoginRequest request) {
        Optional<Breeder> account = breederRepository.findByEmailIgnoreCase(request.email());

        // Gdy konto nie istnieje, i tak porównujemy hasło (z hashem-atrapą), żeby czas odpowiedzi
        // nie zdradzał, czy podany adres e-mail jest zarejestrowany.
        String passwordHash = account.map(Breeder::getPasswordHash).orElseGet(this::getDummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);

        if (account.isEmpty() || !passwordMatches) {
            throw new BadCredentialsException("Nieprawidłowy adres e-mail lub hasło.");
        }

        Breeder breeder = account.get();

        if (breeder.getStatus() == AccountStatus.PENDING) {
            throw new ForbiddenOperationException("Twoje konto oczekuje jeszcze na akceptację administratora.");
        }

        if (breeder.getStatus() == AccountStatus.BLOCKED) {
            throw new ForbiddenOperationException("Twoje konto zostało zablokowane.");
        }

        return jwtService.generateToken(breeder);
    }

    private String getDummyPasswordHash() {
        if (dummyPasswordHash == null) {
            dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        }

        return dummyPasswordHash;
    }

    public CurrentUserDto getCurrentUser(String userEmail) {
        Breeder breeder = breederRepository.findByEmail(userEmail).orElseThrow(() -> new BadCredentialsException("Wymagane jest uwierzytelnienie."));

        if (breeder.getStatus() != AccountStatus.ACTIVE) {
            throw new BadCredentialsException("Wymagane jest uwierzytelnienie.");
        }

        return new CurrentUserDto(
                breeder.getId(),
                breeder.getEmail(),
                breeder.getName(),
                breeder.getRole(),
                breeder.getStatus()
        );
    }
}