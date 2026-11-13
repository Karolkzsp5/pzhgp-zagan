package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.Role;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Dane zalogowanego użytkownika wyświetlane na stronie ustawień.
 *
 * @param boardFunctions funkcje w zarządzie, np. „Prezes – Zarząd Oddziału”; pusta lista, gdy użytkownik nie jest w zarządzie
 */
public record AccountDto(
        String name,
        String surname,
        LocalDate dateOfBirth,
        String email,
        String phoneNumber,
        String postalCode,
        String city,
        String street,
        String houseNumber,
        String sectionName,
        Role role,
        Instant createdAt,
        List<String> boardFunctions
) {}