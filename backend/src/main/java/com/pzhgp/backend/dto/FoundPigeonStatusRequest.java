package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.FoundPigeonStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Żądanie zmiany statusu zgłoszenia przez administratora.
 */
public record FoundPigeonStatusRequest(

        @NotNull(message = "Podaj nowy status zgłoszenia.")
        FoundPigeonStatus status
) {
}
