package com.pzhgp.backend.service.gpx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrackSimplifierTest {

    @Test
    @DisplayName("Punkty leżące na prostej są usuwane, końce trasy zachowane")
    void removesCollinearPoints() {
        List<GpxPoint> points = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            points.add(new GpxPoint(51.0 + i * 0.001, 15.0, 100.0, null));
        }

        List<GpxPoint> simplified = TrackSimplifier.simplify(points, 10.0);

        assertEquals(2, simplified.size());
        assertEquals(points.getFirst(), simplified.getFirst());
        assertEquals(points.getLast(), simplified.getLast());
    }

    @Test
    @DisplayName("Ostry zakręt jest zachowywany")
    void keepsSharpTurn() {
        List<GpxPoint> points = List.of(
                new GpxPoint(51.0, 15.0, null, null),
                new GpxPoint(51.05, 15.0, null, null),
                new GpxPoint(51.1, 15.0, null, null),
                new GpxPoint(51.1, 15.1, null, null),
                new GpxPoint(51.1, 15.2, null, null));

        List<GpxPoint> simplified = TrackSimplifier.simplify(points, 10.0);

        assertEquals(3, simplified.size());
        assertEquals(51.1, simplified.get(1).latitude(), 1e-9);
        assertEquals(15.0, simplified.get(1).longitude(), 1e-9);
    }

    @Test
    @DisplayName("Tolerancja zero zwraca trasę bez zmian")
    void zeroToleranceKeepsEveryPoint() {
        List<GpxPoint> points = List.of(
                new GpxPoint(51.0, 15.0, null, null),
                new GpxPoint(51.05, 15.0, null, null),
                new GpxPoint(51.1, 15.0, null, null));

        assertEquals(3, TrackSimplifier.simplify(points, 0).size());
        assertEquals(List.of(0, 1, 2), TrackSimplifier.selectIndices(points, 0));
    }

    @Test
    @DisplayName("Zwracane indeksy są rosnące i wskazują na punkty oryginalnej trasy")
    void returnsAscendingIndices() {
        List<GpxPoint> points = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            points.add(new GpxPoint(51.0 + i * 0.002, 15.0 + Math.sin(i / 5.0) * 0.01, null, null));
        }

        List<Integer> indices = TrackSimplifier.selectIndices(points, 25.0);

        assertEquals(0, indices.getFirst());
        assertEquals(points.size() - 1, indices.getLast());
        for (int i = 1; i < indices.size(); i++) {
            assertTrue(indices.get(i) > indices.get(i - 1), "Indeksy muszą być rosnące");
        }
    }

    @Test
    @DisplayName("Rzeczywista trasa 1230 punktów zostaje wyraźnie odchudzona bez utraty kształtu")
    void reducesRealTrack() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/gpx/skyleader-lot-konkursowy.gpx")) {
            assertNotNull(stream);
            List<GpxPoint> points = new GpxParser().parse(stream).points();

            List<GpxPoint> simplified = TrackSimplifier.simplify(points, 10.0);

            assertTrue(simplified.size() < points.size() / 2,
                    "Oczekiwano redukcji o ponad połowę, otrzymano " + simplified.size() + " z " + points.size());

            // Maksymalne odchylenie pominiętych punktów od uproszczonej linii nie może
            // przekroczyć zadanej tolerancji.
            double worstDeviation = maxDeviation(points, TrackSimplifier.selectIndices(points, 10.0));
            assertTrue(worstDeviation <= 10.0 + 1e-6,
                    "Odchylenie " + worstDeviation + " m przekracza tolerancję 10 m");
        }
    }

    private double maxDeviation(List<GpxPoint> points, List<Integer> keptIndices) {
        double worst = 0.0;
        for (int k = 1; k < keptIndices.size(); k++) {
            GpxPoint start = points.get(keptIndices.get(k - 1));
            GpxPoint end = points.get(keptIndices.get(k));
            for (int i = keptIndices.get(k - 1) + 1; i < keptIndices.get(k); i++) {
                GpxPoint point = points.get(i);
                worst = Math.max(worst, GeoMath.distanceToSegment(
                        point.latitude(), point.longitude(),
                        start.latitude(), start.longitude(),
                        end.latitude(), end.longitude()));
            }
        }
        return worst;
    }
}
