package com.pzhgp.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * Żądanie zapisania prywatnej notatki administratora.
 * Pusta wartość czyści notatkę.
 */
public record FoundPigeonNoteRequest(

        @Size(max = 2000, message = "Notatka może mieć maksymalnie 2000 znaków.")
        String adminNote
) {
}
