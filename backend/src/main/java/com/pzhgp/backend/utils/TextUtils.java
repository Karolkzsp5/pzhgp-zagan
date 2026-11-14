package com.pzhgp.backend.utils;

public final class TextUtils {

    private TextUtils() {
    }

    /**
     * Usuwa białe znaki z początku i końca tekstu. Pola żądań przycina się przed walidacją,
     * żeby {@code @NotBlank} i {@code @Size} oceniały wartość, która trafi do bazy.
     *
     * @return przycięty tekst albo {@code null}, gdy przekazano {@code null}
     */
    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Usuwa białe znaki z początku i końca tekstu.
     *
     * @return przycięty tekst albo {@code null}, gdy wartość jest pusta lub zawiera same białe znaki
     */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
