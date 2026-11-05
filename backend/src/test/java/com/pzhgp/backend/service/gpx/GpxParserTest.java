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
    @DisplayName("Should parse a standard GPX 1.1 file")
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
    @DisplayName("Should parse a Skyleader GPX file without namespace and version attributes")
    void parsesSkyleaderFileWithoutNamespace() throws IOException {
        try (InputStream stream = resource("skyleader-minimalny.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertEquals(5, track.points().size());
            assertEquals("Ring-8414", track.trackName());
            assertEquals("8414", track.ringNumber());
        }
    }

    @Test
    @DisplayName("Should parse all track points from a real Skyleader GPX export")
    void parsesRealSkyleaderExport() throws IOException {
        try (InputStream stream = resource("skyleader-lot-konkursowy.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertEquals(1230, track.points().size());
            assertEquals("8414", track.ringNumber());
            assertEquals(Instant.parse("2026-08-30T04:12:35Z"), track.points().getFirst().time());
            assertEquals(Instant.parse("2026-08-30T09:18:48Z"), track.points().getLast().time());
        }
    }

    @Test
    @DisplayName("Should interpret timestamps without a time zone as UTC")
    void treatsTimeWithoutZoneAsUtc() throws IOException {
        try (InputStream stream = resource("skyleader-minimalny.gpx")) {
            GpxTrack track = parser.parse(stream);
            assertEquals(Instant.parse("2026-08-30T05:00:00Z"), track.points().getFirst().time());
        }
    }

    @Test
    @DisplayName("Should ignore metadata timestamps when reading track point times")
    void ignoresMetadataTime() throws IOException {
        try (InputStream stream = resource("skyleader-minimalny.gpx")) {
            GpxTrack track = parser.parse(stream);
            assertTrue(track.points().stream().allMatch(point -> point.time().isAfter(Instant.parse("2020-01-01T00:00:00Z"))));
        }
    }

    @Test
    @DisplayName("Should read the creator name from metadata link text when the root attribute is missing")
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
    @DisplayName("Should return null ring number when the track has no name")
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
    @DisplayName("Should assign null elevation and timestamps to track points missing those fields")
    void parsesPointsWithoutElevationAndTime() throws IOException {
        try (InputStream stream = resource("bez-czasu.gpx")) {
            GpxTrack track = parser.parse(stream);

            assertEquals(3, track.points().size());
            assertNull(track.points().getFirst().time());
            assertNull(track.points().getFirst().elevation());
        }
    }

    @Test
    @DisplayName("Should store null for invalid optional elevation and timestamp values")
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
    @DisplayName("Should skip points with malformed, non-finite or out-of-range coordinates")
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
    @DisplayName("Should skip coordinates outside valid Earth ranges")
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
    @DisplayName("Should block external XML entities")
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
    @DisplayName("Should reject a GPX file without track points")
    void rejectsFileWithoutTrackPoints() throws IOException {
        try (InputStream stream = resource("bez-punktow.gpx")) {
            GpxParsingException exception = assertThrows(GpxParsingException.class, () -> parser.parse(stream));
            assertTrue(exception.getMessage().contains("trkpt"));
        }
    }

    @Test
    @DisplayName("Should throw a readable GPX parsing exception for malformed XML")
    void rejectsMalformedXml() {
        InputStream stream = new ByteArrayInputStream(
                "<gpx><trk><trkseg><trkpt lat=".getBytes(StandardCharsets.UTF_8));

        GpxParsingException exception = assertThrows(GpxParsingException.class, () -> parser.parse(stream));
        assertTrue(exception.getMessage().contains("GPX"));
    }

    @Test
    @DisplayName("Should reject a GPX file exceeding the maximum number of track points")
    void rejectsTooManyTrackPoints() {
        StringBuilder gpx = new StringBuilder("<gpx><trk><trkseg>");

        for (int i = 0; i <= GpxParser.MAX_POINTS; i++) {
            gpx.append("<trkpt lat=\"51.0\" lon=\"15.0\"/>");
        }

        gpx.append("</trkseg></trk></gpx>");

        GpxParsingException exception = assertThrows(GpxParsingException.class, () -> parser.parse(stream(gpx.toString())));
        assertTrue(exception.getMessage().contains(String.valueOf(GpxParser.MAX_POINTS)));
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