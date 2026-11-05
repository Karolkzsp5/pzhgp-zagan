package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Role;

import java.time.Instant;
import java.time.LocalDate;

public record BreederResponseDto(
        Long id,
        String name,
        String surname,
        String email,
        String phoneNumber,
        LocalDate dateOfBirth,
        String postalCode,
        String city,
        String street,
        String houseNumber,
        Long sectionId,
        String sectionName,
        AccountStatus status,
        Role role,
        Instant createdAt
) {}