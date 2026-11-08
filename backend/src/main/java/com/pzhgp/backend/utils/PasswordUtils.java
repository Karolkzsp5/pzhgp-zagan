package com.pzhgp.backend.utils;

import java.nio.charset.StandardCharsets;

public final class PasswordUtils {

    /** BCrypt uwzględnia wyłącznie pierwsze 72 bajty hasła — dłuższe hasło zostałoby po cichu obcięte. */
    public static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private PasswordUtils() {
    }

    /** Sprawdza, czy hasło zapisane w UTF-8 mieści się w limicie BCrypt; brak hasła ocenia osobna walidacja. */
    public static boolean fitsBcryptLimit(String password) {
        return password == null || (password.length() <= BCRYPT_MAX_PASSWORD_BYTES && password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_PASSWORD_BYTES);
    }
}
