package com.pzhgp.backend.dto;

import jakarta.validation.constraints.AssertTrue;

/**
 * Nowy status wątku. Pole pominięte (null) zostaje bez zmian, więc ponowne wysłanie
 * tego samego żądania niczego nie przełącza.
 */
public record ThreadStatusRequest(
        Boolean isLocked,
        Boolean isPinned
) {

    @AssertTrue(message = "Podaj nowy status wątku.")
    public boolean isStatusProvided() {
        return isLocked != null || isPinned != null;
    }
}
