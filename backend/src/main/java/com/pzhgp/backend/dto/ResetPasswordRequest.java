package com.pzhgp.backend.dto;

import com.pzhgp.backend.utils.PasswordUtils;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Ustawienie nowego hasła po otwarciu linku z wiadomości e-mail.
 *
 * @param token    jednorazowy token z linku
 * @param password nowe hasło; obowiązują te same reguły co przy rejestracji
 */
public record ResetPasswordRequest(
        @NotBlank(message = "Link do zmiany hasła jest nieprawidłowy lub wygasł. Poproś o nowy link.")
        String token,

        @NotBlank(message = "Hasło nie może być puste.")
        @Pattern(regexp = PasswordUtils.STRONG_PASSWORD_REGEX, message = PasswordUtils.STRONG_PASSWORD_MESSAGE)
        String password
) {
    @AssertTrue(message = "Hasło może mieć maksymalnie 72 bajty (UTF-8).")
    public boolean isPasswordLengthValid() {
        return PasswordUtils.fitsBcryptLimit(password);
    }
}