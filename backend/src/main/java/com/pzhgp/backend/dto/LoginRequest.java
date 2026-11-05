package com.pzhgp.backend.dto;

import com.pzhgp.backend.utils.PasswordUtils;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record LoginRequest(
        @NotBlank(message = "Adres e-mail nie może być pusty")
        @Email(message = "Niepoprawny format adresu e-mail")
        String email,

        @NotBlank(message = "Hasło nie może być puste")
        String password,

        // Gdy true, ciasteczko z tokenem przetrwa zamknięcie przeglądarki. Brak pola oznacza false.
        Boolean rememberMe
) {
    public LoginRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
        rememberMe = Boolean.TRUE.equals(rememberMe);
    }

    @AssertTrue(message = "Hasło może mieć maksymalnie 72 bajty (UTF-8).")
    public boolean isPasswordLengthValid() {
        return PasswordUtils.fitsBcryptLimit(password);
    }
}