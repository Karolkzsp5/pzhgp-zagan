package com.pzhgp.backend.dto;

import com.pzhgp.backend.utils.PasswordUtils;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Zmiana hasła przez zalogowanego użytkownika.
 *
 * @param currentPassword obecne hasło; chroni konto, gdy ktoś inny ma dostęp do zalogowanej przeglądarki
 * @param newPassword     nowe hasło; obowiązują te same reguły co przy rejestracji
 */
public record PasswordChangeRequest(
        @NotBlank(message = "Podaj obecne hasło.")
        String currentPassword,

        @NotBlank(message = "Hasło nie może być puste.")
        @Pattern(regexp = PasswordUtils.STRONG_PASSWORD_REGEX, message = PasswordUtils.STRONG_PASSWORD_MESSAGE)
        String newPassword
) {
    @AssertTrue(message = "Hasło może mieć maksymalnie 72 bajty (UTF-8).")
    public boolean isNewPasswordLengthValid() {
        return PasswordUtils.fitsBcryptLimit(newPassword);
    }
}