package com.pzhgp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Zmiana danych kontaktowych przez użytkownika na stronie ustawień.
 * Obowiązują te same reguły co przy rejestracji. Imię, nazwisko, datę urodzenia, e-mail i sekcję zmienia administrator.
 */
public record ContactUpdateRequest(
        @NotBlank(message = "Numer telefonu nie może być pusty.")
        @Pattern(regexp = "^\\d{9}$", message = "Numer telefonu musi mieć dokładnie 9 cyfr.")
        String phoneNumber,

        @NotBlank(message = "Kod pocztowy nie może być pusty.")
        @Pattern(regexp = "^\\d{2}-\\d{3}$", message = "Niepoprawny format kodu pocztowego (wymagany: XX-XXX).")
        String postalCode,

        @NotBlank(message = "Miejscowość nie może być pusta.")
        @Size(max = 100, message = "Miejscowość może mieć maksymalnie 100 znaków.")
        String city,

        @NotBlank(message = "Ulica nie może być pusta.")
        @Size(max = 100, message = "Ulica może mieć maksymalnie 100 znaków.")
        String street,

        @NotBlank(message = "Numer domu/lokalu nie może być pusty.")
        @Size(max = 10, message = "Numer domu/lokalu może mieć maksymalnie 10 znaków.")
        @Pattern(regexp = "^[1-9]\\d*\\s?[a-zA-Z]?(\\s?[\\/-]\\s?[1-9]\\d*\\s?[a-zA-Z]?)?$", message = "Podaj poprawny numer domu/lokalu (np. 12, 12A, 12/4).")
        String houseNumber
) {}