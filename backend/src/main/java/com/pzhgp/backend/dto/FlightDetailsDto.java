package com.pzhgp.backend.dto;

import java.time.Instant;
import java.util.List;

/**
 * Pełne dane lotu wraz z trasą — podstawa widoku mapy.
 *
 * @param trackPoints    punkty trasy (opcjonalnie uproszczone algorytmem RDP)
 * @param returnedPoints liczba punktów faktycznie zwróconych po uproszczeniu
 */
public record FlightDetailsDto(
        Long id,
        String name,
        String ringNumber,
        String releaseSite,
        String ownerName,
        String originalFileName,
        Instant startTime,
        Instant endTime,
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude,
        FlightStatisticsDto statistics,
        List<FlightTrackPointDto> trackPoints,
        int returnedPoints,
        Instant uploadedAt,
        boolean canDelete
) {
}
