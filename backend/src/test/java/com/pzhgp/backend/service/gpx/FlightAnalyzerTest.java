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
    @DisplayName("Real Skyleader flight")
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
        @DisplayName("Should analyze the entire track from the first to the last GPX point")
        void analyzesWholeTrack() {
            assertEquals(1230, analysis.totalPoints());
            assertEquals(Instant.parse("2026-08-30T04:12:35Z"), analysis.startTime());
            assertEquals(Instant.parse("2026-08-30T09:18:48Z"), analysis.endTime());
            assertEquals(306, analysis.durationSeconds() / 60, 1);
        }

        @Test
        @DisplayName("Should use the first and last GPX points as route endpoints")
        void usesFirstAndLastPointAsEnds() {
            assertEquals(51.7990426, analysis.startLatitude(), 1e-6);
            assertEquals(12.9314796, analysis.startLongitude(), 1e-6);
            assertEquals(51.7041686, analysis.endLatitude(), 1e-6);
            assertEquals(15.606668, analysis.endLongitude(), 1e-6);
        }

        @Test
        @DisplayName("Should calculate approximately 184 km of straight-line distance")
        void computesStraightLineDistance() {
            assertEquals(184.4, analysis.straightLineDistanceMeters() / 1000.0, 1.0);
        }

        @Test
        @DisplayName("Should calculate approximately 195 km along the recorded track")
        void computesTrackDistance() {
            assertEquals(195.3, analysis.trackDistanceMeters() / 1000.0, 1.0);
        }

        @Test
        @DisplayName("Should calculate flight speeds in meters per minute")
        void computesSpeedsInMetersPerMinute() {
            assertEquals(638.0, analysis.averageSpeedMetersPerMinute(), 15.0);
            assertEquals(602.0, analysis.straightLineSpeedMetersPerMinute(), 15.0);
            assertTrue(analysis.maxSpeedMetersPerMinute() > analysis.averageSpeedMetersPerMinute());
        }

        @Test
        @DisplayName("Should keep the calculated maximum speed within a plausible range")
        void computesPlausibleMaxSpeed() {
            assertEquals(1754.0, analysis.maxSpeedMetersPerMinute(), 60.0);
            assertTrue(analysis.maxSpeedMetersPerMinute() < 3333.0);
        }

        @Test
        @DisplayName("Should calculate equal track and straight-line average speeds for a straight flight")
        void straightFlightHasEqualSpeeds() {
            List<GpxPoint> points = straightLine(51.0, 15.0, 0.01, 20, 60);

            FlightAnalysis analysis = analyzer.analyze(points);

            assertEquals(analysis.averageSpeedMetersPerMinute(),
                    analysis.straightLineSpeedMetersPerMinute(), 1.0);
        }

        @Test
        @DisplayName("Should calculate approximately 1000 m/min for one kilometer traveled in one minute")
        void computesKnownSpeed() {
            Instant base = Instant.parse("2026-08-30T05:00:00Z");

            List<GpxPoint> points = List.of(
                    new GpxPoint(51.0, 15.0, 100.0, base),
                    new GpxPoint(51.0089932, 15.0, 100.0, base.plusSeconds(60)));

            FlightAnalysis analysis = analyzer.analyze(points);

            assertEquals(1000.0, analysis.averageSpeedMetersPerMinute(), 5.0);
            assertEquals(60, analysis.durationSeconds());
        }

        @Test
        @DisplayName("Should use segment speed for sparse samples when no sustained-speed window is available")
        void usesSegmentSpeedFallbackForSparseSamples() {
            Instant base = Instant.parse("2026-08-30T05:00:00Z");

            List<GpxPoint> points = List.of(
                    new GpxPoint(51.0, 15.0, 100.0, base),
                    new GpxPoint(51.0089932, 15.0, 100.0, base.plusSeconds(120)));

            FlightAnalysis analysis = analyzer.analyze(points);

            assertEquals(500.0, analysis.maxSpeedMetersPerMinute(), 5.0);
        }

        @Test
        @DisplayName("Should prevent an implausible GPS position spike from exceeding the maximum speed limit")
        void rejectsImplausibleSpeedSpike() {
            List<GpxPoint> points = new ArrayList<>(straightLine(51.0, 15.0, 0.005, 10, 30));
            Instant last = points.getLast().time();

            points.add(new GpxPoint(51.5, 15.0, 120.0, last.plusSeconds(30)));
            points.add(new GpxPoint(51.055, 15.0, 120.0, last.plusSeconds(60)));

            FlightAnalysis analysis = analyzer.analyze(points);

            assertTrue(analysis.maxSpeedMetersPerMinute() <= 3333.0);
        }

        @Test
        @DisplayName("Should exclude an isolated GPS altitude spike from elevation statistics")
        void filtersElevationOutliers() {
            assertNotNull(analysis.maxElevationMeters());
            assertTrue(analysis.maxElevationMeters() < 500);
            assertNotNull(analysis.elevationGainMeters());
        }
    }

    @Test
    @DisplayName("Should remove an isolated elevation spike using the median filter")
    void filtersSingleElevationSpike() {
        List<GpxPoint> points = List.of(
                pointWithElevation(51.000, 100.0),
                pointWithElevation(51.001, 100.0),
                pointWithElevation(51.002, 1500.0),
                pointWithElevation(51.003, 100.0),
                pointWithElevation(51.004, 100.0));

        FlightAnalysis analysis = analyzer.analyze(points);

        assertEquals(100.0, analysis.minElevationMeters());
        assertEquals(100.0, analysis.maxElevationMeters());
        assertEquals(0.0, analysis.elevationGainMeters());
    }

    @Test
    @DisplayName("Should ignore elevation changes below the 15-meter hysteresis threshold")
    void ignoresSmallElevationChanges() {
        List<GpxPoint> points = List.of(
                pointWithElevation(51.000, 100.0),
                pointWithElevation(51.001, 105.0),
                pointWithElevation(51.002, 110.0),
                pointWithElevation(51.003, 112.0));

        FlightAnalysis analysis = analyzer.analyze(points);

        assertEquals(0.0, analysis.elevationGainMeters());
    }

    @Test
    @DisplayName("Should count climbs exceeding the 15-meter hysteresis threshold in elevation gain")
    void countsSignificantElevationGain() {
        List<GpxPoint> points = List.of(
                pointWithElevation(51.000, 100.0),
                pointWithElevation(51.001, 110.0),
                pointWithElevation(51.002, 120.0),
                pointWithElevation(51.003, 105.0));

        FlightAnalysis analysis = analyzer.analyze(points);

        assertEquals(20.0, analysis.elevationGainMeters());
    }

    @Test
    @DisplayName("Should ignore non-finite elevation values")
    void ignoresNonFiniteElevationValues() {
        List<GpxPoint> points = List.of(
                pointWithElevation(51.000, Double.NaN),
                pointWithElevation(51.001, Double.POSITIVE_INFINITY),
                pointWithElevation(51.002, 100.0),
                pointWithElevation(51.003, 120.0));

        FlightAnalysis analysis = analyzer.analyze(points);

        assertEquals(100.0, analysis.minElevationMeters());
        assertEquals(120.0, analysis.maxElevationMeters());
        assertEquals(20.0, analysis.elevationGainMeters());
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Should reject a track containing fewer than two points")
        void rejectsTooShortTrack() {
            List<GpxPoint> single = List.of(new GpxPoint(51.0, 15.0, 100.0, Instant.now()));

            assertThrows(GpxParsingException.class, () -> analyzer.analyze(single));
            assertThrows(GpxParsingException.class, () -> analyzer.analyze(List.of()));
            assertThrows(GpxParsingException.class, () -> analyzer.analyze(null));
        }

        @Test
        @DisplayName("Should retain distances without computing time-based statistics when timestamps are missing")
        void handlesTrackWithoutTimestamps() {
            List<GpxPoint> points = List.of(
                    new GpxPoint(51.0, 15.0, null, null),
                    new GpxPoint(51.1, 15.0, null, null),
                    new GpxPoint(51.2, 15.0, null, null));

            FlightAnalysis analysis = analyzer.analyze(points);

            assertFalse(analysis.timestampsAvailable());
            assertEquals(0, analysis.durationSeconds());
            assertEquals(0.0, analysis.averageSpeedMetersPerMinute());
            assertEquals(0.0, analysis.straightLineSpeedMetersPerMinute());
            assertEquals(0.0, analysis.maxSpeedMetersPerMinute());
            assertTrue(analysis.straightLineDistanceMeters() > 20_000);
            assertNull(analysis.elevationGainMeters());
        }

        @Test
        @DisplayName("Should omit time-based statistics when the end time is not later than the start time")
        void handlesNonIncreasingEndTime() {
            Instant base = Instant.parse("2026-08-30T05:00:00Z");

            List<GpxPoint> points = List.of(
                    new GpxPoint(51.0, 15.0, 100.0, base),
                    new GpxPoint(51.01, 15.0, 100.0, base.minusSeconds(30)));

            FlightAnalysis analysis = analyzer.analyze(points);

            assertFalse(analysis.timestampsAvailable());
            assertEquals(0, analysis.durationSeconds());
            assertEquals(0.0, analysis.averageSpeedMetersPerMinute());
            assertEquals(0.0, analysis.maxSpeedMetersPerMinute());
        }

        @Test
        @DisplayName("Should analyze a track with missing intermediate timestamps when endpoints have valid times")
        void handlesPartiallyMissingTimestamps() {
            Instant base = Instant.parse("2026-08-30T05:00:00Z");

            List<GpxPoint> points = List.of(
                    new GpxPoint(51.0, 15.0, 100.0, base),
                    new GpxPoint(51.005, 15.0, 100.0, null),
                    new GpxPoint(51.01, 15.0, 100.0, base.plusSeconds(60)));

            FlightAnalysis analysis = analyzer.analyze(points);

            assertTrue(analysis.timestampsAvailable());
            assertEquals(60, analysis.durationSeconds());
            assertTrue(analysis.averageSpeedMetersPerMinute() > 0);
        }
    }

    private List<GpxPoint> straightLine(double startLat, double longitude, double latitudeStep,
                                        int count, int secondsStep) {
        List<GpxPoint> points = new ArrayList<>(count);
        Instant base = Instant.parse("2026-08-30T05:00:00Z");

        for (int i = 0; i < count; i++) {
            points.add(new GpxPoint(startLat + i * latitudeStep, longitude, 120.0,
                    base.plus((long) i * secondsStep, ChronoUnit.SECONDS)));
        }

        return points;
    }

    private GpxPoint pointWithElevation(double latitude, Double elevation) {
        return new GpxPoint(latitude, 15.0, elevation, null);
    }
}