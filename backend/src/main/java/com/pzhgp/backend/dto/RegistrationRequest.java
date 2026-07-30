package com.pzhgp.backend.dto;

public record RegistrationRequest(
        String name,
        String surname,
        String dateOfBirth,
        String postalCode,
        String city,
        String street,
        String houseNumber,
        Integer sectionId,

        String email,
        String phoneNumber,
        String password
) {
}