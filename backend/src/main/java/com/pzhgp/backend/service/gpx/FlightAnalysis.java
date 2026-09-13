package com.pzhgp.backend.service.gpx;

import java.time.Instant;

/**
 * Wynik analizy trasy lotu — komplet statystyk prezentowanych hodowcy.
 * <p>
 * Prędkości wyrażone są w metrach na minutę, czyli w jednostce używanej
 * w regulaminach lotowych PZHGP i na listach konkursowych.
 *
 * @param totalPoints                       liczba punktów odczytanych z pliku
 * @param startTime                         czas pierwszego punktu trasy (UTC)
 * @param endTime                           czas ostatniego punktu trasy (UTC)
 * @param startLatitude                     szerokość geograficzna początku trasy
 * @param startLongitude                    długość geograficzna początku trasy
 * @param endLatitude                       szerokość geograficzna końca trasy
 * @param endLongitude                      długość geograficzna końca trasy
 * @param straightLineDistanceMeters        odległość w linii prostej z początku na koniec trasy
 * @param trackDistanceMeters               droga pokonana wzdłuż całej zarejestrowanej trasy
 * @param durationSeconds                   czas między pierwszym a ostatnim punktem trasy
 * @param averageSpeedMetersPerMinute       prędkość średnia liczona po trasie
 * @param straightLineSpeedMetersPerMinute  prędkość liczona po linii prostej
 * @param maxSpeedMetersPerMinute           najwyższa prędkość utrzymana w oknie czasowym
 * @param minElevationMeters                najniższa wysokość na trasie ({@code null} bez danych)
 * @param maxElevationMeters                najwyższa wysokość na trasie ({@code null} bez danych)
 * @param elevationGainMeters               suma wznosów z histerezą ({@code null} bez danych)
 * @param timestampsAvailable               czy punkty trasy miały znaczniki czasu
 */
public record FlightAnalysis(
        int totalPoints,
        Instant startTime,
        Instant endTime,
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude,
        double straightLineDistanceMeters,
        double trackDistanceMeters,
        long durationSeconds,
        double averageSpeedMetersPerMinute,
        double straightLineSpeedMetersPerMinute,
        double maxSpeedMetersPerMinute,
        Double minElevationMeters,
        Double maxElevationMeters,
        Double elevationGainMeters,
        boolean timestampsAvailable
) {
}
