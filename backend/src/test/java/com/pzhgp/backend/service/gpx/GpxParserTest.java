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

    @Test
    @DisplayName("Parses a Skyleader GPX file without namespace and version attributes")
    void parsesSkyleaderFileWithoutNamespace() throws IOException {
        try (InputStream stream = resource("skyleader-minimalny.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertEquals(5, track.points().size());
            assertEquals("Ring-8414", track.trackName());
            assertEquals("8414", track.ringNumber());
        }
    }

    @Test
    @DisplayName("Interprets timestamps without a time zone as UTC")
    void treatsTimeWithoutZoneAsUtc() throws IOException {
        try (InputStream stream = resource("skyleader-minimalny.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertEquals(Instant.parse("2026-08-30T05:00:00Z"), track.points().getFirst().time());
        }
    }

    @Test
    @DisplayName("Ignores metadata timestamp when reading track point timestamps")
    void ignoresMetadataTime() throws IOException {
        try (InputStream stream = resource("skyleader-minimalny.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertTrue(track.points().stream()
                    .allMatch(point -> point.time().isAfter(Instant.parse("2020-01-01T00:00:00Z"))));
        }
    }

    @Test
    @DisplayName("Reads creator name from metadata link text when root creator attribute is missing")
    void readsCreatorFromMetadataText() {
        String gpx = """
                <gpx>
                    <metadata>
                        <link href="https://example.com">
                            <text>MX Corp.</text>
                        </link>
                    </metadata>
                    <trk>
                        <name>Ring-8414</name>
                        <trkseg>
                            <trkpt lat="51.0" lon="15.0"/>
                        </trkseg>
                    </trk>
                </gpx>
                """;

        GpxTrack track = parser.parse(stream(gpx));

        assertEquals("MX Corp.", track.creator());
        assertEquals("8414", track.ringNumber());
    }

    @Test
    @DisplayName("Parses a standard GPX 1.1 file")
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
    @DisplayName("Parses track points without elevation and timestamps as null values")
    void parsesPointsWithoutElevationAndTime() throws IOException {
        try (InputStream stream = resource("bez-czasu.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertEquals(3, track.points().size());
            assertNull(track.points().getFirst().time());
            assertNull(track.points().getFirst().elevation());
        }
    }

    @Test
    @DisplayName("Invalid optional elevation and timestamp values are stored as null")
    void ignoresInvalidOptionalValues() {
        String gpx = """
                <gpx>
                    <trk>
                        <trkseg>
                            <trkpt lat="51.0" lon="15.0">
                                <ele>invalid</ele>
                                <time>invalid-time</time>
                            </trkpt>
                            <trkpt lat="51.1" lon="15.1">
                                <ele>120.5</ele>
                                <time>2026-08-30T05:00:00Z</time>
                            </trkpt>
                        </trkseg>
                    </trk>
                </gpx>
                """;

        GpxTrack track = parser.parse(stream(gpx));

        assertEquals(2, track.points().size());

        GpxPoint first = track.points().getFirst();
        assertNull(first.elevation());
        assertNull(first.time());

        GpxPoint second = track.points().getLast();
        assertEquals(120.5, second.elevation());
        assertEquals(Instant.parse("2026-08-30T05:00:00Z"), second.time());
    }

    @Test
    @DisplayName("Malformed non-finite and out-of-range coordinates are skipped")
    void skipsInvalidCoordinates() {
        String gpx = """
                <gpx>
                    <trk>
                        <trkseg>
                            <trkpt lat="invalid" lon="15.0"/>
                            <trkpt lat="NaN" lon="15.0"/>
                            <trkpt lat="51.0" lon="Infinity"/>
                            <trkpt lat="999" lon="15.0"/>
                            <trkpt lat="51.0" lon="999"/>
                            <trkpt lat="51.0" lon="15.0"/>
                            <trkpt lat="51.1" lon="15.1"/>
                        </trkseg>
                    </trk>
                </gpx>
                """;

        GpxTrack track = parser.parse(stream(gpx));

        assertEquals(2, track.points().size());
        assertEquals(51.0, track.points().getFirst().latitude());
        assertEquals(51.1, track.points().getLast().latitude());
    }

    @Test
    @DisplayName("Track without a name has no extracted ring number")
    void returnsNullRingNumberWithoutTrackName() {
        String gpx = """
                <gpx>
                    <trk>
                        <trkseg>
                            <trkpt lat="51.0" lon="15.0"/>
                        </trkseg>
                    </trk>
                </gpx>
                """;

        GpxTrack track = parser.parse(stream(gpx));

        assertNull(track.trackName());
        assertNull(track.ringNumber());
    }

    @Test
    @DisplayName("External XML entities are blocked")
    void blocksExternalEntities() throws IOException {
        try (InputStream stream = resource("xxe.gpx")) {
            try {
                GpxTrack track = parser.parse(stream);

                assertFalse(track.trackName() != null && track.trackName().contains("root:"));
            } catch (GpxParsingException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }

    @Test
    @DisplayName("GPX file without track points is rejected")
    void rejectsFileWithoutTrackPoints() throws IOException {
        try (InputStream stream = resource("bez-punktow.gpx")) {
            GpxParsingException exception = assertThrows(GpxParsingException.class, () -> parser.parse(stream));

            assertTrue(exception.getMessage().contains("trkpt"));
        }
    }

    @Test
    @DisplayName("Malformed XML produces a readable GPX parsing exception")
    void rejectsMalformedXml() {
        InputStream stream = new ByteArrayInputStream(
                "<gpx><trk><trkseg><trkpt lat=".getBytes(StandardCharsets.UTF_8));

        GpxParsingException exception = assertThrows(GpxParsingException.class, () -> parser.parse(stream));

        assertTrue(exception.getMessage().contains("GPX"));
    }

    @Test
    @DisplayName("Coordinates outside valid Earth ranges are skipped")
    void skipsPointsOutsideEarth() {
        String gpx = """
                <gpx>
                    <trk>
                        <trkseg>
                            <trkpt lat="999" lon="15.0"><ele>10</ele></trkpt>
                            <trkpt lat="51.0" lon="15.0"><ele>10</ele></trkpt>
                            <trkpt lat="51.1" lon="15.1"><ele>20</ele></trkpt>
                        </trkseg>
                    </trk>
                </gpx>
                """;

        GpxTrack track = parser.parse(stream(gpx));

        assertEquals(2, track.points().size());
    }

    @Test
    @DisplayName("GPX file exceeding the maximum track point count is rejected")
    void rejectsTooManyTrackPoints() {
        StringBuilder gpx = new StringBuilder("<gpx><trk><trkseg>");

        for (int i = 0; i <= GpxParser.MAX_POINTS; i++) {
            gpx.append("<trkpt lat=\"51.0\" lon=\"15.0\"/>");
        }

        gpx.append("</trkseg></trk></gpx>");

        GpxParsingException exception = assertThrows(GpxParsingException.class,
                () -> parser.parse(stream(gpx.toString())));

        assertTrue(exception.getMessage().contains(String.valueOf(GpxParser.MAX_POINTS)));
    }

    @Test
    @DisplayName("Parses all points from the real Skyleader GPX export")
    void parsesRealSkyleaderExport() throws IOException {
        try (InputStream stream = resource("skyleader-lot-konkursowy.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertEquals(1230, track.points().size());
            assertEquals("8414", track.ringNumber());
            assertEquals(Instant.parse("2026-08-30T04:12:35Z"), track.points().getFirst().time());
            assertEquals(Instant.parse("2026-08-30T09:18:48Z"), track.points().getLast().time());
        }
    }

    private InputStream resource(String name) {
        InputStream stream = getClass().getResourceAsStream("/gpx/" + name);
        assertNotNull(stream, "Missing test resource: " + name);
        return stream;
    }

    private InputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}