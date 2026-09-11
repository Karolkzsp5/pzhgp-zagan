package com.pzhgp.backend.dto;

/**
 * Komplet statystyk lotu w jednostkach gotowych do wyświetlenia.
 *
 * @param straightLineDistanceKm        odległość w linii prostej wypuszczenie → gołębnik, w km
 * @param trackDistanceKm               droga pokonana wzdłuż trasy w fazie lotu, w km
 * @param rawTrackDistanceKm            suma odcinków całego pliku, razem z szumem GPS, w km
 * @param flightDurationSeconds         czas lotu w sekundach
 * @param totalDurationSeconds          czas objęty całym plikiem w sekundach
 * @param averageSpeedKmh               prędkość średnia po trasie, w km/h
 * @param racingVelocityMetersPerMinute prędkość konkursowa PZHGP, w m/min
 * @param maxSpeedKmh                   prędkość maksymalna utrzymana w oknie czasowym, w km/h
 * @param straightnessRatio             iloraz drogi po trasie i linii prostej
 * @param detourPercent                 nadmiarowa droga względem linii prostej, w procentach
 * @param courseDegrees                 azymut wypuszczenie → gołębnik, w stopniach
 * @param minElevationMeters            najniższa wysokość w fazie lotu
 * @param maxElevationMeters            najwyższa wysokość w fazie lotu
 * @param elevationGainMeters           suma wznosów
 * @param stationaryNoiseKm             droga wygenerowana przez dryf GPS w spoczynku, w km
 * @param preFlightDurationSeconds      czas oczekiwania przed wypuszczeniem
 * @param postFlightDurationSeconds     czas rejestracji po przylocie
 * @param totalPoints                   liczba punktów w pliku
 * @param timestampsAvailable           czy plik zawierał znaczniki czasu
 */
public record FlightStatisticsDto(
        double straightLineDistanceKm,
        double trackDistanceKm,
        double rawTrackDistanceKm,
        long flightDurationSeconds,
        long totalDurationSeconds,
        double averageSpeedKmh,
        double racingVelocityMetersPerMinute,
        double maxSpeedKmh,
        double straightnessRatio,
        double detourPercent,
        double courseDegrees,
        Double minElevationMeters,
        Double maxElevationMeters,
        Double elevationGainMeters,
        double stationaryNoiseKm,
        long preFlightDurationSeconds,
        long postFlightDurationSeconds,
        int totalPoints,
        boolean timestampsAvailable
) {
}
