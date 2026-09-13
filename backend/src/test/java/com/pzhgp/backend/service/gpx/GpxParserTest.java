package com.pzhgp.backend.service.gpx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GpxParserTest {

    private GpxParser parser;

    @BeforeEach
    void setUp() {
        parser = new GpxParser();
    }

    private InputStream resource(String name) {
        InputStream stream = getClass().getResourceAsStream("/gpx/" + name);
        assertNotNull(stream, "Brak pliku testowego: " + name);
        return stream;
    }

    @Test
    @DisplayName("Odczytuje plik Skyleader mimo braku przestrzeni nazw i atrybutu version")
    void parsesSkyleaderFileWithoutNamespace() throws IOException {
        try (InputStream stream = resource("skyleader-minimalny.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertEquals(5, track.points().size());
            assertEquals("Ring-8414", track.trackName());
            assertEquals("8414", track.ringNumber());
        }
    }

    @Test
    @DisplayName("Czas bez oznaczenia strefy jest interpretowany jako UTC")
    void treatsTimeWithoutZoneAsUtc() throws IOException {
        try (InputStream stream = resource("skyleader-minimalny.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertEquals(Instant.parse("2026-08-30T05:00:00Z"), track.points().getFirst().time());
        }
    }

    @Test
    @DisplayName("Czas z metadanych (artefakt zerowego tygodnia GPS) nie trafia do punktów trasy")
    void ignoresMetadataTime() throws IOException {
        try (InputStream stream = resource("skyleader-minimalny.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertTrue(track.points().stream()
                    .allMatch(point -> point.time().isAfter(Instant.parse("2020-01-01T00:00:00Z"))));
        }
    }

    @Test
    @DisplayName("Odczytuje również pliki zgodne ze specyfikacją GPX 1.1")
    void parsesStandardGpx() throws IOException {
        try (InputStream stream = resource("standardowy-gpx11.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertEquals(2, track.points().size());
            assertEquals("TestCreator", track.creator());
            assertEquals("PL-0208-24-1234", track.ringNumber());
            assertEquals(Instant.parse("2026-07-15T06:00:00Z"), track.points().getFirst().time());
        }
    }

    @Test
    @DisplayName("Punkty bez wysokości i czasu są wczytywane z wartościami null")
    void parsesPointsWithoutElevationAndTime() throws IOException {
        try (InputStream stream = resource("bez-czasu.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertEquals(3, track.points().size());
            assertNull(track.points().getFirst().time());
            assertNull(track.points().getFirst().elevation());
        }
    }

    @Test
    @DisplayName("Encje zewnętrzne (XXE) są blokowane — plik nie ujawnia zawartości serwera")
    void blocksExternalEntities() throws IOException {
        try (InputStream stream = resource("xxe.gpx")) {
            // Parser albo odrzuci dokument z DTD, albo wczyta go bez rozwinięcia encji.
            try {
                GpxTrack track = parser.parse(stream);
                assertFalse(track.trackName() != null && track.trackName().contains("root:"),
                        "Parser rozwinął encję zewnętrzną — podatność XXE!");
            } catch (GpxParsingException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }

    @Test
    @DisplayName("Plik bez punktów trasy kończy się czytelnym błędem")
    void rejectsFileWithoutTrackPoints() throws IOException {
        try (InputStream stream = resource("bez-punktow.gpx")) {
            GpxParsingException exception = assertThrows(GpxParsingException.class, () -> parser.parse(stream));
            assertTrue(exception.getMessage().contains("trkpt"));
        }
    }

    @Test
    @DisplayName("Uszkodzony XML kończy się czytelnym błędem zamiast wyjątkiem technicznym")
    void rejectsMalformedXml() {
        InputStream stream = new ByteArrayInputStream(
                "<gpx><trk><trkseg><trkpt lat=".getBytes(StandardCharsets.UTF_8));

        GpxParsingException exception = assertThrows(GpxParsingException.class, () -> parser.parse(stream));
        assertTrue(exception.getMessage().contains("GPX"));
    }

    @Test
    @DisplayName("Punkty o współrzędnych spoza zakresu Ziemi są pomijane")
    void skipsPointsOutsideEarth() {
        String gpx = """
                <gpx><trk><trkseg>
                  <trkpt lat="999" lon="15.0"><ele>10</ele></trkpt>
                  <trkpt lat="51.0" lon="15.0"><ele>10</ele></trkpt>
                  <trkpt lat="51.1" lon="15.1"><ele>20</ele></trkpt>
                </trkseg></trk></gpx>
                """;

        GpxTrack track = parser.parse(new ByteArrayInputStream(gpx.getBytes(StandardCharsets.UTF_8)));
        assertEquals(2, track.points().size());
    }

    @Test
    @DisplayName("Rzeczywisty plik z obrączki Skyleader wczytuje komplet 1230 punktów")
    void parsesRealSkyleaderExport() throws IOException {
        try (InputStream stream = resource("skyleader-lot-konkursowy.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertEquals(1230, track.points().size());
            assertEquals("8414", track.ringNumber());
            assertEquals(Instant.parse("2026-08-30T04:12:35Z"), track.points().getFirst().time());
            assertEquals(Instant.parse("2026-08-30T09:18:48Z"), track.points().getLast().time());
        }
    }
}
