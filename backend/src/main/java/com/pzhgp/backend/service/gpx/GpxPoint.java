package com.pzhgp.backend.service.gpx;

import java.time.Instant;

/**
 * Pojedynczy punkt trasy odczytany z pliku GPX.
 *
 * @param latitude  szerokość geograficzna w stopniach
 * @param longitude długość geograficzna w stopniach
 * @param elevation wysokość n.p.m. w metrach ({@code null} gdy plik jej nie zawiera)
 * @param time      znacznik czasu w UTC ({@code null} gdy plik jej nie zawiera)
 */
public record GpxPoint(double latitude, double longitude, Double elevation, Instant time) {
}
