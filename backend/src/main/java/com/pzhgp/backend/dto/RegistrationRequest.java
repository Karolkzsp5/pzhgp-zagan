package com.pzhgp.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank(message = "Imię nie może być puste")
        String name,

        @NotBlank(message = "Nazwisko nie może być puste")
        String surname,

        @NotBlank(message = "Data urodzenia nie może być pusta")
        String dateOfBirth,

        @NotBlank(message = "Kod pocztowy nie może być pusty")
        String postalCode,

        @NotBlank(message = "Miejscowość nie może być pusta")
        String city,

        @NotBlank(message = "Ulica nie może być pusta")
        String street,

        @NotBlank(message = "Numer domu/lokalu nie może być pusty")
        String houseNumber,

        @NotNull(message = "ID sekcji nie może być puste")
        Integer sectionId,

        @NotBlank(message = "Adres e-mail nie może być pusty")
        @Email(message = "Niepoprawny format adresu e-mail")
        String email,

        @NotBlank(message = "Numer telefonu nie może być pusty")
        @Size(min = 9, max = 9, message = "Numer telefonu musi mieć dokładnie 9 cyfr")
        String phoneNumber,

        @NotBlank(message = "Hasło nie może być puste")
        @Size(min = 8, message = "Hasło musi mieć minimum 8 znaków")
        String password
) {
}