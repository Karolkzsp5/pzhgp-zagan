package com.pzhgp.backend.dto;

import java.time.Instant;

/**
 * Skrócony opis lotu prezentowany na liście lotów.
 */
public record FlightSummaryDto(
        Long id,
        String name,
        String ringNumber,
        String releaseSite,
        String ownerName,
        Instant startTime,
        Instant endTime,
        double straightLineDistanceKm,
        long durationSeconds,
        double averageSpeedMetersPerMinute,
        int totalPoints,
        boolean timestampsAvailable,
        Instant uploadedAt,
        boolean canDelete
) {
}
