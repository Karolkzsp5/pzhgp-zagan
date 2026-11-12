package com.pzhgp.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * Prośba o wysłanie linku do zmiany hasła.
 *
 * @param email        adres e-mail konta
 * @param captchaToken token z widżetu Cloudflare Turnstile, sprawdzany przed wysłaniem wiadomości
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Adres e-mail nie może być pusty.")
        @Email(message = "Niepoprawny format adresu e-mail.")
        String email,

        @NotBlank(message = "Dokończ weryfikację CAPTCHA.")
        @Size(max = 2048, message = "Token CAPTCHA jest nieprawidłowy.")
        String captchaToken
) {
    public ForgotPasswordRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}