package com.pzhgp.backend.service.gpx;

import java.util.List;

/**
 * Trasa odczytana z pliku GPX wraz z metadanymi identyfikującymi gołębia.
 *
 * @param trackName  zawartość znacznika {@code <trk><name>} (np. "Ring-8414")
 * @param ringNumber numer obrączki wyłuskany z nazwy trasy ({@code null} gdy nie udało się go rozpoznać)
 * @param creator    zawartość atrybutu {@code creator} lub tekstu z {@code <metadata><link>}
 * @param points     punkty trasy w kolejności występowania w pliku
 */
public record GpxTrack(String trackName, String ringNumber, String creator, List<GpxPoint> points) {
}
