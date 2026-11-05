package com.pzhgp.backend.service.gpx;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Upraszczanie trasy algorytmem Ramera–Douglasa–Peuckera.
 * <p>
 * Obrączka zapisuje pozycję nawet co 5 sekund, więc jeden lot to kilka tysięcy punktów.
 * Przy typowej skali mapy różnica między pełną a uproszczoną trasą jest niewidoczna,
 * a ilość danych przesyłanych do przeglądarki spada kilkukrotnie. Algorytm zachowuje
 * punkty charakterystyczne (ostre zakręty), więc kształt trasy pozostaje wierny.
 */
public final class TrackSimplifier {

    private TrackSimplifier() {
    }

    /**
     * Zwraca indeksy punktów, które należy zachować, aby uproszczona trasa nie odbiegała
     * od oryginalnej o więcej niż {@code toleranceMeters}. Pierwszy i ostatni punkt
     * są zawsze zachowywane.
     *
     * @param points          punkty wejściowe
     * @param toleranceMeters maksymalne dopuszczalne odchylenie w metrach
     */
    public static List<Integer> selectIndices(List<GpxPoint> points, double toleranceMeters) {
        List<Integer> indices = new ArrayList<>();
        if (points == null || points.isEmpty()) {
            return indices;
        }
        if (points.size() < 3 || toleranceMeters <= 0) {
            for (int i = 0; i < points.size(); i++) {
                indices.add(i);
            }
            return indices;
        }

        boolean[] keep = new boolean[points.size()];
        keep[0] = true;
        keep[points.size() - 1] = true;

        Deque<int[]> ranges = new ArrayDeque<>();
        ranges.push(new int[]{0, points.size() - 1});

        while (!ranges.isEmpty()) {
            int[] range = ranges.pop();
            int first = range[0];
            int last = range[1];
            if (last <= first + 1) {
                continue;
            }

            GpxPoint start = points.get(first);
            GpxPoint end = points.get(last);

            double maxDistance = -1;
            int maxIndex = -1;
            for (int i = first + 1; i < last; i++) {
                GpxPoint point = points.get(i);
                double distance = GeoMath.distanceToSegment(
                        point.latitude(), point.longitude(),
                        start.latitude(), start.longitude(),
                        end.latitude(), end.longitude());
                if (distance > maxDistance) {
                    maxDistance = distance;
                    maxIndex = i;
                }
            }

            if (maxDistance > toleranceMeters) {
                keep[maxIndex] = true;
                ranges.push(new int[]{first, maxIndex});
                ranges.push(new int[]{maxIndex, last});
            }
        }

        for (int i = 0; i < points.size(); i++) {
            if (keep[i]) {
                indices.add(i);
            }
        }
        return indices;
    }

    /**
     * Upraszcza trasę, usuwając punkty odległe od uproszczonej linii o mniej niż {@code toleranceMeters}.
     */
    public static List<GpxPoint> simplify(List<GpxPoint> points, double toleranceMeters) {
        if (points == null) {
            return List.of();
        }
        return selectIndices(points, toleranceMeters).stream()
                .map(points::get)
                .toList();
    }
}
