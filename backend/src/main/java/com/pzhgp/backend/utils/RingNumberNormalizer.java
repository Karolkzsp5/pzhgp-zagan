package com.pzhgp.backend.utils;

import java.util.Locale;

/**
 * Sprowadza numer obrączki do postaci porównywalnej.
 * <p>
 * Znalazcy zapisują ten sam numer na wiele sposobów — "PL-0208-24-1234",
 * "pl 0208 24 1234", "PL/0208/24/1234". Aby wyszukiwanie działało niezależnie od
 * zapisu, obok wartości oryginalnej przechowywana jest postać znormalizowana:
 * wielkie litery bez znaków rozdzielających.
 * <p>
 * Usuwane są wyłącznie separatory. Litery, cyfry i zera wiodące pozostają nietknięte,
 * ponieważ niosą informację o kraju, oddziale i roku obrączkowania.
 */
public final class RingNumberNormalizer {

    private RingNumberNormalizer() {
    }

    /**
     * Zwraca numer obrączki bez znaków rozdzielających, zapisany wielkimi literami.
     *
     * @param ringNumber numer w postaci wpisanej przez użytkownika
     * @return postać znormalizowana albo pusty tekst, gdy nie podano numeru
     */
    public static String normalize(String ringNumber) {
        if (ringNumber == null) {
            return "";
        }

        StringBuilder normalized = new StringBuilder(ringNumber.length());
        for (char character : ringNumber.toCharArray()) {
            if (Character.isLetterOrDigit(character)) {
                normalized.append(character);
            }
        }

        return normalized.toString().toUpperCase(Locale.ROOT);
    }
}
