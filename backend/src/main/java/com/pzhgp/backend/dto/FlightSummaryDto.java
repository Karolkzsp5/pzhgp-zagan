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
        Instant releaseTime,
        Instant arrivalTime,
        double straightLineDistanceKm,
        long flightDurationSeconds,
        double averageSpeedKmh,
        double racingVelocityMetersPerMinute,
        int totalPoints,
        Instant uploadedAt,
        boolean canDelete
) {
}
