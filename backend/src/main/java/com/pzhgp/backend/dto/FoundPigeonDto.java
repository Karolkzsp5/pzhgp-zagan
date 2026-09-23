package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.FoundPigeonStatus;
import com.pzhgp.backend.entity.ReportLanguage;

import java.time.LocalDateTime;

/**
 * Pełne dane zgłoszenia widoczne wyłącznie w panelu administratora.
 * <p>
 * Zawiera dane kontaktowe znalazcy oraz prywatną notatkę, dlatego nie wolno go
 * zwracać z żadnego publicznego endpointu.
 */
public record FoundPigeonDto(
        Long id,
        String ringNumber,
        String contactPhone,
        String contactEmail,
        String foundLocation,
        String foundCountry,
        String description,
        ReportLanguage preferredLanguage,
        FoundPigeonStatus status,
        String adminNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
