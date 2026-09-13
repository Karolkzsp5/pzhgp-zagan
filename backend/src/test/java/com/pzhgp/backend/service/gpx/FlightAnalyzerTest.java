package com.pzhgp.backend.service.gpx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlightAnalyzerTest {

    private FlightAnalyzer analyzer;
    private GpxParser parser;

    @BeforeEach
    void setUp() {
        analyzer = new FlightAnalyzer();
        parser = new GpxParser();
    }

    @Nested
    @DisplayName("Rzeczywisty lot z obrączki Skyleader")
    class RealSkyleaderFlight {

        private FlightAnalysis analysis;

        @BeforeEach
        void analyzeRealFile() throws IOException {
            try (InputStream stream = getClass().getResourceAsStream("/gpx/skyleader-lot-konkursowy.gpx")) {
                assertNotNull(stream);
                analysis = analyzer.analyze(parser.parse(stream).points());
            }
        }

        @Test
        @DisplayName("Analizowana jest cała trasa — od pierwszego do ostatniego punktu pliku")
        void analyzesWholeTrack() {
            assertEquals(1230, analysis.totalPoints());
            assertEquals(Instant.parse("2026-08-30T04:12:35Z"), analysis.startTime());
            assertEquals(Instant.parse("2026-08-30T09:18:48Z"), analysis.endTime());
            assertEquals(306, analysis.durationSeconds() / 60, 1);
        }

        @Test
        @DisplayName("Początek i koniec trasy to pierwszy i ostatni punkt pliku")
        void usesFirstAndLastPointAsEnds() {
            assertEquals(51.7990426, analysis.startLatitude(), 1e-6);
            assertEquals(12.9314796, analysis.startLongitude(), 1e-6);
            assertEquals(51.7041686, analysis.endLatitude(), 1e-6);
            assertEquals(15.606668, analysis.endLongitude(), 1e-6);
        }

        @Test
        @DisplayName("Dystans w linii prostej wynosi ok. 184 km")
        void computesStraightLineDistance() {
            assertEquals(184.4, analysis.straightLineDistanceMeters() / 1000, 1.0);
        }

        @Test
        @DisplayName("Droga po trasie obejmuje cały zapis i wynosi ok. 195 km")
        void computesTrackDistance() {
            assertEquals(195.3, analysis.trackDistanceMeters() / 1000, 1.0);
        }

        @Test
        @DisplayName("Prędkości podawane są w metrach na minutę")
        void computesSpeedsInMetersPerMinute() {
            // 195,3 km w ciągu 306 minut to ok. 638 m/min
            assertEquals(638.0, analysis.averageSpeedMetersPerMinute(), 15.0);
            assertEquals(602.0, analysis.straightLineSpeedMetersPerMinute(), 15.0);
            assertTrue(analysis.maxSpeedMetersPerMinute() > analysis.averageSpeedMetersPerMinute());
        }

        @Test
        @DisplayName("Prędkość maksymalna mieści się w zakresie realnym dla gołębia")
        void computesPlausibleMaxSpeed() {
            // ok. 105 km/h to ok. 1754 m/min
            assertEquals(1754.0, analysis.maxSpeedMetersPerMinute(), 60.0);
            assertTrue(analysis.maxSpeedMetersPerMinute() < 3333.0,
                    "Prędkość maksymalna nie może być artefaktem GPS");
        }

        @Test
        @DisplayName("Statystyki wysokości odrzucają pojedynczy skok GPS do 1495 m")
        void filtersElevationOutliers() {
            assertNotNull(analysis.maxElevationMeters());
            assertTrue(analysis.maxElevationMeters() < 500,
                    "Odczyt 1495 m n.p.m. to błąd GPS, a nie rzeczywisty pułap lotu");
            assertNotNull(analysis.elevationGainMeters());
        }
    }

    @Nested
    @DisplayName("Przypadki brzegowe")
    class EdgeCases {

        @Test
        @DisplayName("Trasa krótsza niż dwa punkty jest odrzucana")
        void rejectsTooShortTrack() {
            List<GpxPoint> single = List.of(new GpxPoint(51.0, 15.0, 100.0, Instant.now()));

            assertThrows(GpxParsingException.class, () -> analyzer.analyze(single));
            assertThrows(GpxParsingException.class, () -> analyzer.analyze(List.of()));
            assertThrows(GpxParsingException.class, () -> analyzer.analyze(null));
        }

        @Test
        @DisplayName("Trasa bez znaczników czasu daje dystanse, ale nie prędkości")
        void handlesTrackWithoutTimestamps() {
            List<GpxPoint> points = List.of(
                    new GpxPoint(51.0, 15.0, null, null),
                    new GpxPoint(51.1, 15.0, null, null),
                    new GpxPoint(51.2, 15.0, null, null));

            FlightAnalysis analysis = analyzer.analyze(points);

            assertFalse(analysis.timestampsAvailable());
            assertEquals(0, analysis.durationSeconds());
            assertEquals(0.0, analysis.averageSpeedMetersPerMinute());
            assertTrue(analysis.straightLineDistanceMeters() > 20_000);
            assertNull(analysis.elevationGainMeters());
        }

        @Test
        @DisplayName("Prosty przelot: prędkość średnia równa prędkości po linii prostej")
        void straightFlightHasEqualSpeeds() {
            List<GpxPoint> points = straightLine(51.0, 15.0, 0.01, 20, 60);

            FlightAnalysis analysis = analyzer.analyze(points);

            assertEquals(analysis.averageSpeedMetersPerMinute(),
                    analysis.straightLineSpeedMetersPerMinute(), 1.0);
        }

        @Test
        @DisplayName("Przelot 1 km w minutę daje 1000 m/min")
        void computesKnownSpeed() {
            Instant base = Instant.parse("2026-08-30T05:00:00Z");
            List<GpxPoint> points = List.of(
                    new GpxPoint(51.0, 15.0, 100.0, base),
                    // ok. 1000 m na północ
                    new GpxPoint(51.0089932, 15.0, 100.0, base.plusSeconds(60)));

            FlightAnalysis analysis = analyzer.analyze(points);

            assertEquals(1000.0, analysis.averageSpeedMetersPerMinute(), 5.0);
            assertEquals(60, analysis.durationSeconds());
        }

        @Test
        @DisplayName("Pojedynczy skok pozycji nie zawyża prędkości maksymalnej ponad granicę realności")
        void rejectsImplausibleSpeedSpike() {
            List<GpxPoint> points = new ArrayList<>(straightLine(51.0, 15.0, 0.005, 10, 30));
            Instant last = points.getLast().time();

            // błędny odczyt: skok o ok. 50 km w 30 sekund
            points.add(new GpxPoint(51.5, 15.0, 120.0, last.plusSeconds(30)));
            points.add(new GpxPoint(51.055, 15.0, 120.0, last.plusSeconds(60)));

            FlightAnalysis analysis = analyzer.analyze(points);

            assertTrue(analysis.maxSpeedMetersPerMinute() <= 3333.0,
                    "Odczyty implikujące ponad 3333 m/min muszą zostać odrzucone, otrzymano "
                            + analysis.maxSpeedMetersPerMinute());
        }
    }

    /** Generuje prostą trasę na południku, o zadanym kroku i odstępie czasowym. */
    private List<GpxPoint> straightLine(double startLat, double lon, double latStep, int count, int secondsStep) {
        List<GpxPoint> points = new ArrayList<>(count);
        Instant base = Instant.parse("2026-08-30T05:00:00Z");
        for (int i = 0; i < count; i++) {
            points.add(new GpxPoint(startLat + i * latStep, lon, 120.0,
                    base.plus((long) i * secondsStep, ChronoUnit.SECONDS)));
        }
        return points;
    }
}
