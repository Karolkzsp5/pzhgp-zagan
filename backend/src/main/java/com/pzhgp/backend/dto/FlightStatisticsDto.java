package com.pzhgp.backend.dto;

/**
 * Komplet statystyk lotu w jednostkach gotowych do wyświetlenia.
 * Prędkości podawane są w metrach na minutę — tak jak na listach konkursowych PZHGP.
 *
 * @param straightLineDistanceKm            odległość w linii prostej z początku na koniec trasy, w km
 * @param trackDistanceKm                   droga pokonana wzdłuż całej trasy, w km
 * @param durationSeconds                   czas między pierwszym a ostatnim punktem trasy
 * @param averageSpeedMetersPerMinute       prędkość średnia liczona po trasie
 * @param straightLineSpeedMetersPerMinute  prędkość liczona po linii prostej
 * @param maxSpeedMetersPerMinute           prędkość maksymalna utrzymana w oknie czasowym
 * @param minElevationMeters                najniższa wysokość na trasie
 * @param maxElevationMeters                najwyższa wysokość na trasie
 * @param elevationGainMeters               suma wznosów
 * @param totalPoints                       liczba punktów w pliku
 * @param timestampsAvailable               czy plik zawierał znaczniki czasu
 */
public record FlightStatisticsDto(
        double straightLineDistanceKm,
        double trackDistanceKm,
        long durationSeconds,
        double averageSpeedMetersPerMinute,
        double straightLineSpeedMetersPerMinute,
        double maxSpeedMetersPerMinute,
        Double minElevationMeters,
        Double maxElevationMeters,
        Double elevationGainMeters,
        int totalPoints,
        boolean timestampsAvailable
) {
}
