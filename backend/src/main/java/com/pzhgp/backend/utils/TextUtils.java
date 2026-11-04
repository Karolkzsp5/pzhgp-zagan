package com.pzhgp.backend.utils;

public final class TextUtils {

    private TextUtils() {}

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
