package com.pzhgp.backend.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RegistrationRequest(
        @NotBlank(message = "Imię nie może być puste")
        String name,

        @NotBlank(message = "Nazwisko nie może być puste")
        String surname,

        @NotNull(message = "Data urodzenia nie może być pusta")
        @Past(message = "Data urodzenia musi być z przeszłości")
        LocalDate dateOfBirth,

        @NotBlank(message = "Kod pocztowy nie może być pusty")
        @Pattern(regexp = "^\\d{2}-\\d{3}$", message = "Niepoprawny format kodu pocztowego (wymagany: XX-XXX)")
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
        @Pattern(regexp = "^\\d{9}$", message = "Numer telefonu musi mieć dokładnie 9 cyfr")
        String phoneNumber,

        @NotBlank(message = "Hasło nie może być puste")
        @Size(min = 8, message = "Hasło musi mieć minimum 8 znaków")
        String password
) {
}