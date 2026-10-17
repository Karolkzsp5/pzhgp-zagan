package com.pzhgp.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public record LoginRequest(
        @NotBlank(message = "Adres e-mail nie może być pusty")
        @Email(message = "Niepoprawny format adresu e-mail")
        String email,

        @NotBlank(message = "Hasło nie może być puste")
        String password
) {
    public LoginRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }

    @AssertTrue(message = "Hasło może mieć maksymalnie 72 bajty (UTF-8).")
    public boolean isPasswordLengthValid() {
        return password == null || (password.length() <= 72 && password.getBytes(StandardCharsets.UTF_8).length <= 72);
    }
}