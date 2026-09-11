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
        @DisplayName("Wykrywa moment wypuszczenia po ponad godzinie postoju na miejscu zlotu")
        void detectsRelease() {
            assertEquals(Instant.parse("2026-08-30T05:20:54Z"), analysis.releaseTime());
            assertTrue(analysis.preFlightDurationSeconds() > 3600,
                    "Gołąb czekał na wypuszczenie ponad godzinę");
        }

        @Test
        @DisplayName("Wykrywa moment przylotu, pomijając ponad godzinę rejestracji przy gołębniku")
        void detectsArrival() {
            assertEquals(Instant.parse("2026-08-30T07:34:07Z"), analysis.arrivalTime());
            assertTrue(analysis.postFlightDurationSeconds() > 3600,
                    "Nadajnik rejestrował pozycję jeszcze długo po powrocie");
        }

        @Test
        @DisplayName("Czas lotu to ok. 2 h 13 min, a nie 5 h objętych plikiem")
        void computesFlightDuration() {
            assertEquals(133, analysis.flightDurationSeconds() / 60, 1);
            assertEquals(306, analysis.totalDurationSeconds() / 60, 1);
        }

        @Test
        @DisplayName("Dystans w linii prostej wynosi ok. 184 km")
        void computesStraightLineDistance() {
            assertEquals(184.0, analysis.straightLineDistanceMeters() / 1000, 1.0);
        }

        @Test
        @DisplayName("Prędkość średnia liczona na fazie lotu to ok. 84 km/h, a nie 38 km/h z całego pliku")
        void computesMeaningfulAverageSpeed() {
            assertEquals(84.0, analysis.averageSpeedKmh(), 2.0);

            double naiveSpeed = analysis.rawTrackDistanceMeters() / analysis.totalDurationSeconds() * 3.6;
            assertEquals(38.0, naiveSpeed, 2.0);
            assertTrue(analysis.averageSpeedKmh() > naiveSpeed * 2,
                    "Naiwne liczenie zaniża prędkość ponad dwukrotnie");
        }

        @Test
        @DisplayName("Prędkość konkursowa PZHGP to ok. 1378 m/min")
        void computesRacingVelocity() {
            assertEquals(1378.0, analysis.racingVelocityMetersPerMinute(), 15.0);
        }

        @Test
        @DisplayName("Raportuje kilometry wygenerowane wyłącznie przez dryf GPS w spoczynku")
        void reportsStationaryNoise() {
            assertTrue(analysis.stationaryNoiseMeters() > 5_000,
                    "Postój przed startem i po przylocie dopisał ponad 5 km nieistniejącej drogi");
            assertEquals(analysis.rawTrackDistanceMeters(),
                    analysis.trackDistanceMeters() + analysis.stationaryNoiseMeters(), 1.0);
        }

        @Test
        @DisplayName("Gołąb leciał niemal idealnie prosto — nadkład trasy poniżej 5%")
        void computesStraightness() {
            assertTrue(analysis.straightnessRatio() > 1.0 && analysis.straightnessRatio() < 1.05,
                    "Oczekiwano współczynnika prostoliniowości ok. 1,02, otrzymano "
                            + analysis.straightnessRatio());
        }

        @Test
        @DisplayName("Prędkość maksymalna mieści się w zakresie realnym dla gołębia")
        void computesPlausibleMaxSpeed() {
            assertTrue(analysis.maxSpeedKmh() > analysis.averageSpeedKmh());
            assertTrue(analysis.maxSpeedKmh() < 150, "Prędkość maksymalna nie może być artefaktem GPS");
        }

        @Test
        @DisplayName("Statystyki wysokości pomijają skok GPS do 1495 m zarejestrowany przy gołębniku")
        void filtersElevationOutliers() {
            assertNotNull(analysis.maxElevationMeters());
            assertTrue(analysis.maxElevationMeters() < 400,
                    "Odczyt 1495 m n.p.m. to błąd GPS, a nie rzeczywisty pułap lotu");
            assertNotNull(analysis.elevationGainMeters());
            assertTrue(analysis.elevationGainMeters() < 1_000,
                    "Suma wznosów liczona bez histerezy byłaby zawyżona przez szum");
        }

        @Test
        @DisplayName("Kurs lotu prowadzi na wschód (zachodnie Niemcy → Żagań)")
        void computesCourse() {
            assertEquals(90.0, analysis.courseDegrees(), 20.0);
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
        @DisplayName("Trasa bez postojów jest analizowana w całości")
        void analyzesTrackWithoutStationaryPhases() {
            List<GpxPoint> points = straightLine(51.0, 15.0, 0.01, 20, 60);

            FlightAnalysis analysis = analyzer.analyze(points);

            assertEquals(0, analysis.releaseIndex());
            assertEquals(points.size() - 1, analysis.arrivalIndex());
            assertEquals(0.0, analysis.stationaryNoiseMeters(), 1.0);
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
            assertEquals(0, analysis.flightDurationSeconds());
            assertEquals(0.0, analysis.averageSpeedKmh());
            assertTrue(analysis.straightLineDistanceMeters() > 20_000);
            assertNull(analysis.elevationGainMeters());
        }

        @Test
        @DisplayName("Gołąb, który nigdy nie opuścił gołębnika, nie wywraca analizy")
        void handlesTrackThatNeverLeaves() {
            List<GpxPoint> points = new ArrayList<>();
            Instant time = Instant.parse("2026-08-30T05:00:00Z");
            for (int i = 0; i < 30; i++) {
                points.add(new GpxPoint(
                        51.0 + (i % 3) * 0.00005, 15.0 + (i % 2) * 0.00005, 100.0, time.plusSeconds(i * 5L)));
            }

            FlightAnalysis analysis = analyzer.analyze(points);

            assertEquals(30, analysis.totalPoints());
            assertTrue(analysis.straightLineDistanceMeters() < 100);
            assertTrue(analysis.averageSpeedKmh() < 10);
        }

        @Test
        @DisplayName("Krążenie nad gołębnikiem przed usiądnięciem liczy się jeszcze jako lot")
        void countsCirclingAboveLoftAsFlight() {
            List<GpxPoint> points = new ArrayList<>();
            Instant time = Instant.parse("2026-08-30T05:00:00Z");

            // dolot po linii prostej
            for (int i = 0; i < 40; i++) {
                points.add(new GpxPoint(51.0 + i * 0.01, 15.0, 120.0, time.plusSeconds(i * 30L)));
            }
            Instant afterApproach = time.plusSeconds(40 * 30L);
            double loftLat = 51.0 + 39 * 0.01;

            // dwa okrążenia w promieniu ok. 1 km nad gołębnikiem
            for (int i = 0; i < 12; i++) {
                double angle = 2 * Math.PI * i / 6.0;
                points.add(new GpxPoint(
                        loftLat + 0.009 * Math.cos(angle),
                        15.0 + 0.014 * Math.sin(angle),
                        120.0,
                        afterApproach.plusSeconds(i * 20L)));
            }
            Instant landed = afterApproach.plusSeconds(12 * 20L);

            // siedzenie na gołębniku
            for (int i = 0; i < 20; i++) {
                points.add(new GpxPoint(loftLat + (i % 2) * 0.00003, 15.0, 120.0, landed.plusSeconds(i * 60L)));
            }

            FlightAnalysis analysis = analyzer.analyze(points);

            assertTrue(analysis.arrivalTime().compareTo(landed) >= 0,
                    "Przylot powinien zostać wyznaczony dopiero po zakończeniu krążenia");
            assertTrue(analysis.postFlightDurationSeconds() > 0);
        }

        @Test
        @DisplayName("Pojedynczy skok pozycji nie zawyża prędkości maksymalnej ponad granicę realności")
        void rejectsImplausibleSpeedSpike() {
            List<GpxPoint> points = new ArrayList<>(straightLine(51.0, 15.0, 0.005, 10, 30));
            Instant last = points.getLast().time();

            // błędny odczyt: skok o ok. 50 km w 30 sekund (6000 km/h)
            points.add(new GpxPoint(51.5, 15.0, 120.0, last.plusSeconds(30)));
            points.add(new GpxPoint(51.055, 15.0, 120.0, last.plusSeconds(60)));

            FlightAnalysis analysis = analyzer.analyze(points);

            assertTrue(analysis.maxSpeedKmh() <= 200.0,
                    "Odczyty implikujące prędkość ponad 200 km/h muszą zostać odrzucone, otrzymano "
                            + analysis.maxSpeedKmh());
        }
    }

    /** Generuje prostą trasę na południku, o zadanym kroku i odstępie czasowym. */
    private List<GpxPoint> straightLine(double startLat, double lon, double latStep, int count, int secondsStep) {
        List<GpxPoint> points = new ArrayList<>(count);
        Instant time = Instant.parse("2026-08-30T05:00:00Z");
        for (int i = 0; i < count; i++) {
            points.add(new GpxPoint(startLat + i * latStep, lon, 120.0, time.plus(i * secondsStep, ChronoUnit.SECONDS)));
        }
        return points;
    }
}
