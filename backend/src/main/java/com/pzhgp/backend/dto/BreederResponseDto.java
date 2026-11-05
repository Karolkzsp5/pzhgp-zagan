package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.AccountStatus;

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
        Integer sectionId,
        String sectionName,
        AccountStatus status,
        String role,
        Instant createdAt
) {}