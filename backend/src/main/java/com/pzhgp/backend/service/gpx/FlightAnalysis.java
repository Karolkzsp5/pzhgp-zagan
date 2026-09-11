package com.pzhgp.backend.service.gpx;

import java.time.Instant;

/**
 * Wynik analizy trasy lotu — komplet statystyk prezentowanych hodowcy.
 *
 * @param totalPoints                    liczba punktów odczytanych z pliku
 * @param releaseIndex                   indeks punktu wypuszczenia w liście punktów
 * @param arrivalIndex                   indeks punktu przylotu do gołębnika
 * @param trackStartTime                 czas pierwszego punktu w pliku (UTC)
 * @param trackEndTime                   czas ostatniego punktu w pliku (UTC)
 * @param releaseTime                    wykryty moment wypuszczenia (UTC)
 * @param arrivalTime                    wykryty moment przylotu (UTC)
 * @param releaseLatitude                szerokość geograficzna miejsca wypuszczenia
 * @param releaseLongitude               długość geograficzna miejsca wypuszczenia
 * @param arrivalLatitude                szerokość geograficzna gołębnika
 * @param arrivalLongitude               długość geograficzna gołębnika
 * @param straightLineDistanceMeters     odległość w linii prostej wypuszczenie → gołębnik
 * @param trackDistanceMeters            droga faktycznie pokonana wzdłuż trasy (faza lotu)
 * @param rawTrackDistanceMeters         suma odcinków całego pliku, łącznie z szumem GPS w spoczynku
 * @param flightDurationSeconds          czas lotu od wypuszczenia do przylotu
 * @param totalDurationSeconds           czas objęty całym plikiem
 * @param averageSpeedKmh                prędkość średnia liczona po trasie, w km/h
 * @param racingVelocityMetersPerMinute  prędkość konkursowa PZHGP: linia prosta / czas lotu, w m/min
 * @param maxSpeedKmh                    najwyższa prędkość utrzymana w oknie czasowym, w km/h
 * @param straightnessRatio              iloraz drogi po trasie i linii prostej (1,0 = lot idealnie prosty)
 * @param courseDegrees                  azymut z miejsca wypuszczenia do gołębnika, w stopniach
 * @param minElevationMeters             najniższa wysokość w fazie lotu ({@code null} bez danych)
 * @param maxElevationMeters             najwyższa wysokość w fazie lotu ({@code null} bez danych)
 * @param elevationGainMeters            suma wznosów z histerezą ({@code null} bez danych)
 * @param stationaryNoiseMeters          droga "przebyta" przez szum GPS przed startem i po przylocie
 * @param preFlightDurationSeconds       czas od pierwszego punktu do wypuszczenia (koszyk / miejsce zlotu)
 * @param postFlightDurationSeconds      czas od przylotu do ostatniego punktu (gołębnik)
 * @param timestampsAvailable            czy plik zawierał znaczniki czasu pozwalające liczyć prędkości
 */
public record FlightAnalysis(
        int totalPoints,
        int releaseIndex,
        int arrivalIndex,
        Instant trackStartTime,
        Instant trackEndTime,
        Instant releaseTime,
        Instant arrivalTime,
        double releaseLatitude,
        double releaseLongitude,
        double arrivalLatitude,
        double arrivalLongitude,
        double straightLineDistanceMeters,
        double trackDistanceMeters,
        double rawTrackDistanceMeters,
        long flightDurationSeconds,
        long totalDurationSeconds,
        double averageSpeedKmh,
        double racingVelocityMetersPerMinute,
        double maxSpeedKmh,
        double straightnessRatio,
        double courseDegrees,
        Double minElevationMeters,
        Double maxElevationMeters,
        Double elevationGainMeters,
        double stationaryNoiseMeters,
        long preFlightDurationSeconds,
        long postFlightDurationSeconds,
        boolean timestampsAvailable
) {
}
