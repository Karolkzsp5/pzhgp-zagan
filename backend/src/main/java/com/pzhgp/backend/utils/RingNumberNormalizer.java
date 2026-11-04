package com.pzhgp.backend.utils;

import java.util.Locale;

/**
 * Sprowadza numer obrączki do postaci porównywalnej.
 * <p>
 * Zgłoszenia przyjmowane są wyłącznie w formacie PL-0369-RR-NNNN, ale administrator
 * wyszukuje je po dowolnym fragmencie numeru, wpisanym np. jako "0369 24" albo "241234".
 * Dlatego obok numeru zapisywana jest postać znormalizowana — wielkie litery bez znaków
 * rozdzielających — i w ten sam sposób normalizowana jest szukana fraza.
 * <p>
 * Usuwane są wyłącznie separatory. Litery, cyfry i zera wiodące pozostają nietknięte,
 * ponieważ niosą informację o kraju, oddziale i roku obrączkowania.
 */
public final class RingNumberNormalizer {

    private RingNumberNormalizer() {}

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
