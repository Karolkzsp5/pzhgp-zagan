package com.pzhgp.backend.utils;

public final class PaginationUtils {

    private PaginationUtils() {}

    public static void validate(int page, int size, int maxSize) {
        if (page < 0) {
            throw new IllegalArgumentException("Numer strony nie może być ujemny.");
        }

        if (size < 1 || size > maxSize) {
            throw new IllegalArgumentException("Rozmiar strony musi mieścić się w zakresie od 1 do " + maxSize + ".");
        }
    }
}