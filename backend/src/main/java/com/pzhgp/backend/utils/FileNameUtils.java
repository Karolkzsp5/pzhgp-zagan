package com.pzhgp.backend.utils;

public final class FileNameUtils {

    private static final int MAX_FILE_NAME_LENGTH = 255;

    private FileNameUtils() {}

    /**
     * Usuwa z nazwy pliku elementy ścieżki, aby nazwa pochodząca od użytkownika
     * nie mogła wskazywać na inny katalog, i skraca ją do długości kolumny w bazie.
     *
     * @param fileName nazwa pliku przesłana przez przeglądarkę
     * @param fallback nazwa używana, gdy przesłana nazwa jest pusta
     */
    public static String sanitize(String fileName, String fallback) {
        if (fileName == null || fileName.isBlank()) {
            return fallback;
        }

        String bare = fileName.replaceAll(".*[/\\\\]", "").trim();
        if (bare.isBlank()) {
            return fallback;
        }

        return bare.length() > MAX_FILE_NAME_LENGTH ? bare.substring(bare.length() - MAX_FILE_NAME_LENGTH) : bare;
    }
}
