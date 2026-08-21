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
        @Pattern(regexp = "^[1-9]\\d*\\s?[a-zA-Z]?(\\s?[\\/-]\\s?[1-9]\\d*\\s?[a-zA-Z]?)?$",
                message = "Podaj poprawny numer domu/lokalu (np. 12, 12A, 12/4)")
        String houseNumber,

        @NotNull(message = "ID sekcji nie może być puste")
        Integer sectionId,

        @NotBlank(message = "Adres e-mail nie może być pusty")
        @Email(message = "Niepoprawny format adresu e-mail")
        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                message = "Wymagany poprawny adres e-mail z domeną")
        String email,

        @NotBlank(message = "Numer telefonu nie może być pusty")
        @Pattern(regexp = "^\\d{9}$", message = "Numer telefonu musi mieć dokładnie 9 cyfr")
        String phoneNumber,

        @NotBlank(message = "Hasło nie może być puste")
        @Pattern(
                regexp = "^(?=.*[a-zżźćńółęąś])(?=.*[A-ZŻŹĆŃÓŁĘĄŚ])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>_\\-=+/\\\\]).{8,}$",
                message = "Hasło musi mieć min. 8 znaków, zawierać dużą i małą literę, cyfrę oraz znak specjalny"
        )
        String password
) {
}