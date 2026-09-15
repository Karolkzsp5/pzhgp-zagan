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
        @DisplayName("Analyzes the entire track from the first to the last GPX point")
        void analyzesWholeTrack() {
            assertEquals(1230, analysis.totalPoints());
            assertEquals(Instant.parse("2026-08-30T04:12:35Z"), analysis.startTime());
            assertEquals(Instant.parse("2026-08-30T09:18:48Z"), analysis.endTime());
            assertEquals(306, analysis.durationSeconds() / 60, 1);
        }

        @Test
        @DisplayName("Uses the first and last GPX points as the route endpoints")
        void usesFirstAndLastPointAsEnds() {
            assertEquals(51.7990426, analysis.startLatitude(), 1e-6);
            assertEquals(12.9314796, analysis.startLongitude(), 1e-6);
            assertEquals(51.7041686, analysis.endLatitude(), 1e-6);
            assertEquals(15.606668, analysis.endLongitude(), 1e-6);
        }

        @Test
        @DisplayName("Calculates approximately 184 kilometers of straight-line distance")
        void computesStraightLineDistance() {
            assertEquals(184.4, analysis.straightLineDistanceMeters() / 1000.0, 1.0);
        }

        @Test
        @DisplayName("Calculates approximately 195 kilometers along the recorded track")
        void computesTrackDistance() {
            assertEquals(195.3, analysis.trackDistanceMeters() / 1000.0, 1.0);
        }

        @Test
        @DisplayName("Calculates flight speeds in meters per minute")
        void computesSpeedsInMetersPerMinute() {
            assertEquals(638.0, analysis.averageSpeedMetersPerMinute(), 15.0);
            assertEquals(602.0, analysis.straightLineSpeedMetersPerMinute(), 15.0);
            assertTrue(analysis.maxSpeedMetersPerMinute() > analysis.averageSpeedMetersPerMinute());
        }

        @Test
        @DisplayName("Calculated maximum speed remains within a plausible range")
        void computesPlausibleMaxSpeed() {
            assertEquals(1754.0, analysis.maxSpeedMetersPerMinute(), 60.0);
            assertTrue(analysis.maxSpeedMetersPerMinute() < 3333.0);
        }

        @Test
        @DisplayName("Elevation statistics reject the isolated GPS altitude spike")
        void filtersElevationOutliers() {
            assertNotNull(analysis.maxElevationMeters());
            assertTrue(analysis.maxElevationMeters() < 500);
            assertNotNull(analysis.elevationGainMeters());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Rejects a track containing fewer than two points")
        void rejectsTooShortTrack() {
            List<GpxPoint> single = List.of(new GpxPoint(51.0, 15.0, 100.0, Instant.now()));

            assertThrows(GpxParsingException.class, () -> analyzer.analyze(single));
            assertThrows(GpxParsingException.class, () -> analyzer.analyze(List.of()));
            assertThrows(GpxParsingException.class, () -> analyzer.analyze(null));
        }

        @Test
        @DisplayName("Track without timestamps keeps distances but has no time-based statistics")
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
        @DisplayName("Non-increasing end time disables time-based statistics")
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
        @DisplayName("Straight flight has equal track and straight-line average speeds")
        void straightFlightHasEqualSpeeds() {
            List<GpxPoint> points = straightLine(51.0, 15.0, 0.01, 20, 60);

            FlightAnalysis analysis = analyzer.analyze(points);

            assertEquals(analysis.averageSpeedMetersPerMinute(),
                    analysis.straightLineSpeedMetersPerMinute(), 1.0);
        }

        @Test
        @DisplayName("One kilometer traveled in one minute produces approximately 1000 meters per minute")
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
        @DisplayName("Sparse samples use segment speed when no sustained-speed window is available")
        void usesSegmentSpeedFallbackForSparseSamples() {
            Instant base = Instant.parse("2026-08-30T05:00:00Z");

            List<GpxPoint> points = List.of(
                    new GpxPoint(51.0, 15.0, 100.0, base),
                    new GpxPoint(51.0089932, 15.0, 100.0, base.plusSeconds(120)));

            FlightAnalysis analysis = analyzer.analyze(points);

            assertEquals(500.0, analysis.maxSpeedMetersPerMinute(), 5.0);
        }

        @Test
        @DisplayName("Missing timestamps inside the track do not prevent analysis when endpoints have valid timestamps")
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

        @Test
        @DisplayName("Implausible GPS position spike does not increase maximum speed above the configured limit")
        void rejectsImplausibleSpeedSpike() {
            List<GpxPoint> points = new ArrayList<>(straightLine(51.0, 15.0, 0.005, 10, 30));
            Instant last = points.getLast().time();

            points.add(new GpxPoint(51.5, 15.0, 120.0, last.plusSeconds(30)));
            points.add(new GpxPoint(51.055, 15.0, 120.0, last.plusSeconds(60)));

            FlightAnalysis analysis = analyzer.analyze(points);

            assertTrue(analysis.maxSpeedMetersPerMinute() <= 3333.0);
        }

        @Test
        @DisplayName("Median filter removes an isolated elevation spike")
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
        @DisplayName("Elevation gain ignores changes below the fifteen-meter hysteresis threshold")
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
        @DisplayName("Elevation gain counts climbs exceeding the fifteen-meter hysteresis threshold")
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
        @DisplayName("Non-finite elevation values are ignored")
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