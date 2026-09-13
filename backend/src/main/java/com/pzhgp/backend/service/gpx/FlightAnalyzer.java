package com.pzhgp.backend.service.gpx;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wylicza statystyki lotu na podstawie punktów odczytanych z pliku GPX.
 * <p>
 * Analizowana jest cała zarejestrowana trasa — od pierwszego do ostatniego punktu pliku.
 * Początek trasy to miejsce, w którym nadajnik zaczął zapisywać pozycję, a koniec trasy —
 * ostatni zapisany odczyt.
 * <p>
 * Prędkości wyrażane są w metrach na minutę, czyli w jednostce, w której hodowcy odczytują
 * wyniki na listach konkursowych.
 */
@Component
public class FlightAnalyzer {

    /** Liczba sekund w minucie — przelicznik prędkości na metry na minutę. */
    private static final double SECONDS_PER_MINUTE = 60.0;

    /** Minimalna długość okna czasowego przy liczeniu prędkości maksymalnej, w sekundach. */
    private static final long MAX_SPEED_MIN_WINDOW_SECONDS = 15;

    /** Maksymalna długość okna czasowego przy liczeniu prędkości maksymalnej, w sekundach. */
    private static final long MAX_SPEED_MAX_WINDOW_SECONDS = 60;

    /**
     * Prędkość, powyżej której odczyt uznajemy za błąd GPS, a nie za lot gołębia.
     * 3333 m/min to ok. 200 km/h — ponad dwukrotnie więcej niż rekordowe przeloty.
     */
    private static final double IMPLAUSIBLE_SPEED_METERS_PER_MINUTE = 3333.0;

    /** Szerokość okna mediany wygładzającej wysokość. */
    private static final int ELEVATION_MEDIAN_WINDOW = 5;

    /** Histereza sumowania wznosów w metrach — odfiltrowuje pionowy szum GPS. */
    private static final double ELEVATION_GAIN_THRESHOLD_METERS = 15.0;

    /**
     * Analizuje całą trasę zapisaną w pliku.
     *
     * @throws GpxParsingException gdy trasa zawiera mniej niż dwa punkty
     */
    public FlightAnalysis analyze(List<GpxPoint> points) {
        if (points == null || points.size() < 2) {
            throw new GpxParsingException("Trasa musi zawierać co najmniej dwa punkty, aby wyznaczyć statystyki lotu.");
        }

        GpxPoint start = points.getFirst();
        GpxPoint end = points.getLast();

        double trackDistance = pathLength(points);
        double straightLineDistance = GeoMath.distance(
                start.latitude(), start.longitude(), end.latitude(), end.longitude());

        long duration = durationBetween(start.time(), end.time());
        boolean timestampsAvailable = duration > 0;

        double averageSpeed = timestampsAvailable ? metersPerMinute(trackDistance, duration) : 0.0;
        double straightLineSpeed = timestampsAvailable ? metersPerMinute(straightLineDistance, duration) : 0.0;
        double maxSpeed = timestampsAvailable ? findMaxSustainedSpeed(points) : 0.0;

        ElevationStats elevation = analyzeElevation(points);

        return new FlightAnalysis(
                points.size(),
                start.time(),
                end.time(),
                start.latitude(),
                start.longitude(),
                end.latitude(),
                end.longitude(),
                straightLineDistance,
                trackDistance,
                duration,
                averageSpeed,
                straightLineSpeed,
                maxSpeed,
                elevation.min(),
                elevation.max(),
                elevation.gain(),
                timestampsAvailable
        );
    }

    /**
     * Najwyższa prędkość utrzymana przez okno co najmniej {@value #MAX_SPEED_MIN_WINDOW_SECONDS} s.
     * Uśrednianie po oknie zamiast po pojedynczym odcinku eliminuje skoki wynikające z błędu
     * pozycji: przy odczytach co 5 s dryf o 30 m daje pozornie 360 m/min "z niczego".
     */
    private double findMaxSustainedSpeed(List<GpxPoint> points) {
        double best = 0.0;

        for (int i = 0; i < points.size(); i++) {
            GpxPoint from = points.get(i);
            if (from.time() == null) {
                continue;
            }
            for (int j = i + 1; j < points.size(); j++) {
                GpxPoint to = points.get(j);
                if (to.time() == null) {
                    continue;
                }
                long seconds = Duration.between(from.time(), to.time()).getSeconds();
                if (seconds < MAX_SPEED_MIN_WINDOW_SECONDS) {
                    continue;
                }
                if (seconds > MAX_SPEED_MAX_WINDOW_SECONDS) {
                    break;
                }
                double speed = metersPerMinute(GeoMath.distance(
                        from.latitude(), from.longitude(), to.latitude(), to.longitude()), seconds);
                if (speed > best && speed <= IMPLAUSIBLE_SPEED_METERS_PER_MINUTE) {
                    best = speed;
                }
            }
        }

        return best > 0 ? best : maxSegmentSpeed(points);
    }

    /**
     * Zapasowy sposób liczenia prędkości maksymalnej dla plików o rzadkim próbkowaniu,
     * w których żadne okno czasowe nie mieści się w dopuszczalnym zakresie.
     */
    private double maxSegmentSpeed(List<GpxPoint> points) {
        double best = 0.0;
        for (int i = 1; i < points.size(); i++) {
            GpxPoint previous = points.get(i - 1);
            GpxPoint current = points.get(i);
            if (previous.time() == null || current.time() == null) {
                continue;
            }
            long seconds = Duration.between(previous.time(), current.time()).getSeconds();
            if (seconds <= 0) {
                continue;
            }
            double speed = metersPerMinute(GeoMath.distance(
                    previous.latitude(), previous.longitude(), current.latitude(), current.longitude()), seconds);
            if (speed > best && speed <= IMPLAUSIBLE_SPEED_METERS_PER_MINUTE) {
                best = speed;
            }
        }
        return best;
    }

    /**
     * Statystyki wysokości liczone na wygładzonym medianą przebiegu. Surowe odczyty
     * barometryczno-satelitarne potrafią w pojedynczym punkcie skoczyć o ponad kilometr,
     * dlatego sumowanie wznosów prowadzone jest z histerezą.
     */
    private ElevationStats analyzeElevation(List<GpxPoint> points) {
        List<Double> raw = new ArrayList<>();
        for (GpxPoint point : points) {
            Double elevation = point.elevation();
            if (elevation != null && Double.isFinite(elevation)) {
                raw.add(elevation);
            }
        }
        if (raw.isEmpty()) {
            return new ElevationStats(null, null, null);
        }

        List<Double> smoothed = medianFilter(raw);

        double min = smoothed.getFirst();
        double max = smoothed.getFirst();
        double gain = 0.0;
        double reference = smoothed.getFirst();

        for (double value : smoothed) {
            min = Math.min(min, value);
            max = Math.max(max, value);

            if (value > reference + ELEVATION_GAIN_THRESHOLD_METERS) {
                gain += value - reference;
                reference = value;
            } else if (value < reference) {
                reference = value;
            }
        }

        return new ElevationStats(min, max, gain);
    }

    private List<Double> medianFilter(List<Double> values) {
        if (values.size() < ELEVATION_MEDIAN_WINDOW) {
            return values;
        }
        int radius = ELEVATION_MEDIAN_WINDOW / 2;
        List<Double> result = new ArrayList<>(values.size());

        for (int i = 0; i < values.size(); i++) {
            int from = Math.max(0, i - radius);
            int to = Math.min(values.size(), i + radius + 1);
            double[] window = new double[to - from];
            for (int j = from; j < to; j++) {
                window[j - from] = values.get(j);
            }
            Arrays.sort(window);
            result.add(window[window.length / 2]);
        }
        return result;
    }

    /** Suma długości wszystkich odcinków trasy, w metrach. */
    private double pathLength(List<GpxPoint> points) {
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            GpxPoint previous = points.get(i - 1);
            GpxPoint current = points.get(i);
            total += GeoMath.distance(
                    previous.latitude(), previous.longitude(), current.latitude(), current.longitude());
        }
        return total;
    }

    private long durationBetween(Instant from, Instant to) {
        if (from == null || to == null || !to.isAfter(from)) {
            return 0L;
        }
        return Duration.between(from, to).getSeconds();
    }

    /** Przelicza dystans i czas na prędkość w metrach na minutę. */
    private double metersPerMinute(double meters, long seconds) {
        return seconds > 0 ? meters / (seconds / SECONDS_PER_MINUTE) : 0.0;
    }

    private record ElevationStats(Double min, Double max, Double gain) {
    }
}
