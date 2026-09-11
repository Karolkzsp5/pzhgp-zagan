package com.pzhgp.backend.service.gpx;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wylicza statystyki lotu na podstawie surowej listy punktów GPX.
 * <p>
 * <b>Dlaczego to nie jest zwykłe sumowanie odcinków.</b> Nadajnik w obrączce rejestruje pozycję
 * również wtedy, gdy gołąb siedzi w koszu przed wypuszczeniem oraz po powrocie do gołębnika.
 * W obu fazach gołąb stoi w miejscu, ale odbiornik GPS dryfuje o kilkanaście metrów co odczyt,
 * więc naiwne zsumowanie odcinków dopisuje kilka kilometrów nieistniejącej drogi, a podzielenie
 * jej przez czas objęty całym plikiem zaniża prędkość średnią nawet dwukrotnie.
 * <p>
 * Analizator wykrywa więc najpierw <i>fazę lotu</i> (od wypuszczenia do przylotu) i dopiero na
 * niej liczy dystans, prędkości oraz przewyższenia. Droga wygenerowana przez szum w spoczynku
 * raportowana jest osobno jako {@code stationaryNoiseMeters}.
 */
@Component
public class FlightAnalyzer {

    /** Minimalny promień strefy startu/gołębnika w metrach. */
    private static final double MIN_ANCHOR_RADIUS_METERS = 150.0;

    /** Maksymalny promień strefy startu/gołębnika w metrach. */
    private static final double MAX_ANCHOR_RADIUS_METERS = 500.0;

    /** Promień strefy jako ułamek odległości startu od gołębnika. */
    private static final double ANCHOR_RADIUS_FRACTION = 0.02;

    /** Minimalna długość okna czasowego przy liczeniu prędkości maksymalnej, w sekundach. */
    private static final long MAX_SPEED_MIN_WINDOW_SECONDS = 15;

    /** Maksymalna długość okna czasowego przy liczeniu prędkości maksymalnej, w sekundach. */
    private static final long MAX_SPEED_MAX_WINDOW_SECONDS = 60;

    /** Prędkość, powyżej której odczyt uznajemy za błąd GPS, a nie za lot gołębia, w km/h. */
    private static final double IMPLAUSIBLE_SPEED_KMH = 200.0;

    /** Szerokość okna mediany wygładzającej wysokość. */
    private static final int ELEVATION_MEDIAN_WINDOW = 5;

    /** Histereza sumowania wznosów w metrach — odfiltrowuje pionowy szum GPS. */
    private static final double ELEVATION_GAIN_THRESHOLD_METERS = 15.0;

    /**
     * Analizuje trasę i zwraca komplet statystyk.
     *
     * @throws GpxParsingException gdy trasa zawiera mniej niż dwa punkty
     */
    public FlightAnalysis analyze(List<GpxPoint> points) {
        if (points == null || points.size() < 2) {
            throw new GpxParsingException("Trasa musi zawierać co najmniej dwa punkty, aby wyznaczyć statystyki lotu.");
        }

        int lastIndex = points.size() - 1;
        double[] startAnchor = medianPosition(points, 0, anchorWindow(points.size()));
        double[] endAnchor = medianPosition(points, points.size() - anchorWindow(points.size()), points.size());

        double anchorSeparation = GeoMath.distance(startAnchor[0], startAnchor[1], endAnchor[0], endAnchor[1]);
        double radius = clamp(ANCHOR_RADIUS_FRACTION * anchorSeparation,
                MIN_ANCHOR_RADIUS_METERS, MAX_ANCHOR_RADIUS_METERS);

        // Fazę postoju wyznaczamy tylko wtedy, gdy punkty na początku (odpowiednio na końcu)
        // trasy rzeczywiście tworzą skupisko. Dla pliku obejmującego sam przelot, bez oczekiwania
        // w koszu, mediana pozycji wypadłaby w środku przelatywanego odcinka i obcięłaby trasę.
        int window = anchorWindow(points.size());
        int releaseIndex = isStationaryCluster(points, 0, window, startAnchor, radius)
                ? findReleaseIndex(points, startAnchor, radius)
                : 0;
        int arrivalIndex = isStationaryCluster(points, points.size() - window, points.size(), endAnchor, radius)
                ? findArrivalIndex(points, endAnchor, radius)
                : lastIndex;

        // Zabezpieczenie dla tras, na których nie da się rozdzielić faz (np. sam przelot
        // bez postoju albo trening w obrębie jednego gołębnika) — analizujemy wtedy cały plik.
        if (releaseIndex >= arrivalIndex) {
            releaseIndex = 0;
            arrivalIndex = lastIndex;
        }

        GpxPoint release = points.get(releaseIndex);
        GpxPoint arrival = points.get(arrivalIndex);

        double trackDistance = pathLength(points, releaseIndex, arrivalIndex);
        double rawTrackDistance = pathLength(points, 0, lastIndex);
        double straightLineDistance = GeoMath.distance(
                release.latitude(), release.longitude(), arrival.latitude(), arrival.longitude());

        Instant trackStartTime = points.get(0).time();
        Instant trackEndTime = points.get(lastIndex).time();
        boolean timestampsAvailable = release.time() != null && arrival.time() != null
                && arrival.time().isAfter(release.time());

        long flightDuration = timestampsAvailable
                ? Duration.between(release.time(), arrival.time()).getSeconds() : 0L;
        long totalDuration = trackStartTime != null && trackEndTime != null && trackEndTime.isAfter(trackStartTime)
                ? Duration.between(trackStartTime, trackEndTime).getSeconds() : 0L;

        double averageSpeedKmh = flightDuration > 0 ? metersPerSecondToKmh(trackDistance / flightDuration) : 0.0;
        double racingVelocity = flightDuration > 0 ? straightLineDistance / (flightDuration / 60.0) : 0.0;
        double maxSpeedKmh = timestampsAvailable ? findMaxSustainedSpeedKmh(points, releaseIndex, arrivalIndex) : 0.0;

        double straightness = straightLineDistance > 0 ? trackDistance / straightLineDistance : 0.0;
        double course = GeoMath.bearing(
                release.latitude(), release.longitude(), arrival.latitude(), arrival.longitude());

        double stationaryNoise = pathLength(points, 0, releaseIndex) + pathLength(points, arrivalIndex, lastIndex);
        long preFlightDuration = durationBetween(points.get(0).time(), release.time());
        long postFlightDuration = durationBetween(arrival.time(), points.get(lastIndex).time());

        ElevationStats elevation = analyzeElevation(points, releaseIndex, arrivalIndex);

        return new FlightAnalysis(
                points.size(),
                releaseIndex,
                arrivalIndex,
                trackStartTime,
                trackEndTime,
                release.time(),
                arrival.time(),
                release.latitude(),
                release.longitude(),
                arrival.latitude(),
                arrival.longitude(),
                straightLineDistance,
                trackDistance,
                rawTrackDistance,
                flightDuration,
                totalDuration,
                averageSpeedKmh,
                racingVelocity,
                maxSpeedKmh,
                straightness,
                course,
                elevation.min(),
                elevation.max(),
                elevation.gain(),
                stationaryNoise,
                preFlightDuration,
                postFlightDuration,
                timestampsAvailable
        );
    }

    /**
     * Sprawdza, czy punkty z podanego zakresu tworzą skupisko wokół zadanego środka,
     * czyli czy gołąb faktycznie stał w miejscu.
     */
    private boolean isStationaryCluster(List<GpxPoint> points, int fromIndex, int toIndex,
                                        double[] anchor, double radius) {
        int from = Math.max(0, fromIndex);
        int to = Math.min(points.size(), toIndex);

        int inside = 0;
        int total = 0;
        for (int i = from; i < to; i++) {
            GpxPoint point = points.get(i);
            total++;
            if (GeoMath.distance(anchor[0], anchor[1], point.latitude(), point.longitude()) <= radius) {
                inside++;
            }
        }

        return total > 0 && inside * 2 > total;
    }

    /**
     * Moment wypuszczenia to ostatni punkt leżący jeszcze w strefie miejsca zlotu — po nim
     * gołąb oddala się i już do tej strefy nie wraca.
     */
    private int findReleaseIndex(List<GpxPoint> points, double[] startAnchor, double radius) {
        int release = 0;
        for (int i = 0; i < points.size(); i++) {
            GpxPoint point = points.get(i);
            if (GeoMath.distance(startAnchor[0], startAnchor[1], point.latitude(), point.longitude()) <= radius) {
                release = i;
            }
        }
        return release;
    }

    /**
     * Moment przylotu to pierwszy punkt, od którego gołąb pozostaje już na stałe w strefie
     * gołębnika. Dzięki temu krążenie nad gołębnikiem przed usiądnięciem nie jest mylnie
     * uznane za koniec lotu.
     */
    private int findArrivalIndex(List<GpxPoint> points, double[] endAnchor, double radius) {
        int arrival = points.size() - 1;
        for (int i = points.size() - 1; i >= 0; i--) {
            GpxPoint point = points.get(i);
            if (GeoMath.distance(endAnchor[0], endAnchor[1], point.latitude(), point.longitude()) > radius) {
                break;
            }
            arrival = i;
        }
        return arrival;
    }

    /**
     * Najwyższa prędkość utrzymana przez okno co najmniej {@value #MAX_SPEED_MIN_WINDOW_SECONDS} s.
     * Uśrednianie po oknie zamiast po pojedynczym odcinku eliminuje skoki wynikające z błędu
     * pozycji: przy odczytach co 5 s dryf o 30 m daje pozornie 21 km/h "z niczego".
     */
    private double findMaxSustainedSpeedKmh(List<GpxPoint> points, int fromIndex, int toIndex) {
        double best = 0.0;

        for (int i = fromIndex; i <= toIndex; i++) {
            GpxPoint from = points.get(i);
            if (from.time() == null) {
                continue;
            }
            for (int j = i + 1; j <= toIndex; j++) {
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
                double speed = metersPerSecondToKmh(GeoMath.distance(
                        from.latitude(), from.longitude(), to.latitude(), to.longitude()) / seconds);
                if (speed > best && speed <= IMPLAUSIBLE_SPEED_KMH) {
                    best = speed;
                }
            }
        }

        return best > 0 ? best : maxSegmentSpeedKmh(points, fromIndex, toIndex);
    }

    /**
     * Zapasowy sposób liczenia prędkości maksymalnej dla plików o rzadkim próbkowaniu,
     * w których żadne okno czasowe nie mieści się w dopuszczalnym zakresie.
     */
    private double maxSegmentSpeedKmh(List<GpxPoint> points, int fromIndex, int toIndex) {
        double best = 0.0;
        for (int i = fromIndex + 1; i <= toIndex; i++) {
            GpxPoint previous = points.get(i - 1);
            GpxPoint current = points.get(i);
            if (previous.time() == null || current.time() == null) {
                continue;
            }
            long seconds = Duration.between(previous.time(), current.time()).getSeconds();
            if (seconds <= 0) {
                continue;
            }
            double speed = metersPerSecondToKmh(GeoMath.distance(
                    previous.latitude(), previous.longitude(), current.latitude(), current.longitude()) / seconds);
            if (speed > best && speed <= IMPLAUSIBLE_SPEED_KMH) {
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
    private ElevationStats analyzeElevation(List<GpxPoint> points, int fromIndex, int toIndex) {
        List<Double> raw = new ArrayList<>();
        for (int i = fromIndex; i <= toIndex; i++) {
            Double elevation = points.get(i).elevation();
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

    /**
     * Mediana współrzędnych z podanego zakresu punktów. Mediana, a nie średnia, ponieważ
     * jest odporna na pojedyncze odczyty odstające.
     */
    private double[] medianPosition(List<GpxPoint> points, int fromIndex, int toIndex) {
        int from = Math.max(0, fromIndex);
        int to = Math.min(points.size(), toIndex);

        double[] latitudes = new double[to - from];
        double[] longitudes = new double[to - from];
        for (int i = from; i < to; i++) {
            latitudes[i - from] = points.get(i).latitude();
            longitudes[i - from] = points.get(i).longitude();
        }
        Arrays.sort(latitudes);
        Arrays.sort(longitudes);

        return new double[]{latitudes[latitudes.length / 2], longitudes[longitudes.length / 2]};
    }

    private int anchorWindow(int pointCount) {
        return (int) clamp(pointCount / 20.0, 3, 20);
    }

    private double pathLength(List<GpxPoint> points, int fromIndex, int toIndex) {
        double total = 0.0;
        for (int i = fromIndex + 1; i <= toIndex; i++) {
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

    private double metersPerSecondToKmh(double metersPerSecond) {
        return metersPerSecond * 3.6;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ElevationStats(Double min, Double max, Double gain) {
    }
}
