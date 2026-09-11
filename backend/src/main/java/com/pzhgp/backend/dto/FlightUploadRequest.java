package com.pzhgp.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Metadane podawane przez hodowcę przy wgrywaniu pliku GPX. Wszystkie pola są opcjonalne —
 * nazwa lotu i numer obrączki są odczytywane z pliku, gdy użytkownik ich nie poda.
 */
public record FlightUploadRequest(
        @Size(max = 150, message = "Nazwa lotu może mieć maksymalnie 150 znaków.")
        String name,

        @Size(max = 32, message = "Numer obrączki może mieć maksymalnie 32 znaki.")
        @Pattern(regexp = "^[A-Za-z0-9\\-/ ]*$", message = "Numer obrączki zawiera niedozwolone znaki.")
        String ringNumber,

        @Size(max = 150, message = "Nazwa miejsca wypuszczenia może mieć maksymalnie 150 znaków.")
        String releaseSite
) {
}
