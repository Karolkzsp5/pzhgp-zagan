package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.AccountStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
        LocalDateTime createdAt
) {
}