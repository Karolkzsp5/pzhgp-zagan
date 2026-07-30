package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.LoginRequest;
import com.pzhgp.backend.dto.RegistrationRequest;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.repository.BreederRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BreederService {

    private final BreederRepository breederRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void registerNewBreeder(RegistrationRequest request) {

        if (breederRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Użytkownik z podanym adresem e-mail już istnieje!");
        }

        if (request.phoneNumber() != null && breederRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new IllegalArgumentException("Użytkownik z podanym numerem telefonu już istnieje!");
        }

        Breeder newBreeder = new Breeder();
        newBreeder.setName(request.name());
        newBreeder.setSurname(request.surname());
        newBreeder.setEmail(request.email());
        newBreeder.setPhoneNumber(request.phoneNumber());

        // Parsowanie daty z formatu tekstowego
        if (request.dateOfBirth() != null && !request.dateOfBirth().isEmpty()) {
            newBreeder.setDateOfBirth(java.time.LocalDate.parse(request.dateOfBirth()));
        }

        newBreeder.setPostalCode(request.postalCode());
        newBreeder.setCity(request.city());
        newBreeder.setStreet(request.street());
        newBreeder.setHouseNumber(request.houseNumber());
        newBreeder.setSectionId(request.sectionId());

        String hashedPassword = passwordEncoder.encode(request.password());
        newBreeder.setPasswordHash(hashedPassword);

        breederRepository.save(newBreeder);
    }

    public String login(LoginRequest request) {
        Breeder breeder = breederRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy adres e-mail lub hasło."));

        if (!passwordEncoder.matches(request.password(), breeder.getPasswordHash())) {
            throw new IllegalArgumentException("Nieprawidłowy adres e-mail lub hasło.");
        }

        if (breeder.getStatus() == AccountStatus.PENDING) {
            throw new IllegalStateException("Twoje konto oczekuje jeszcze na akceptację administratora.");
        }
        if (breeder.getStatus() == AccountStatus.BLOCKED || breeder.getStatus() == AccountStatus.REJECTED) {
            throw new IllegalStateException("Twoje konto zostało zablokowane lub odrzucone.");
        }

        return jwtService.generateToken(breeder);
    }
}
