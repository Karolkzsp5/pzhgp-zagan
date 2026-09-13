package com.pzhgp.backend.dto;

import java.time.Instant;

/**
 * Punkt trasy przesyłany do przeglądarki.
 *
 * @param latitude              szerokość geograficzna
 * @param longitude             długość geograficzna
 * @param elevation             wysokość n.p.m. w metrach
 * @param time                  znacznik czasu w UTC
 * @param speedMetersPerMinute  prędkość chwilowa w metrach na minutę
 */
public record FlightTrackPointDto(
        double latitude,
        double longitude,
        Double elevation,
        Instant time,
        Double speedMetersPerMinute
) {
}
