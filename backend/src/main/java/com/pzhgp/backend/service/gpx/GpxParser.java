package com.pzhgp.backend.service.gpx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strumieniowy parser plików GPX oparty o StAX.
 * <p>
 * Celowo nie korzysta z gotowej biblioteki GPX, ponieważ pliki eksportowane przez
 * oprogramowanie obrączek Skyleader odbiegają od specyfikacji Topografix GPX 1.1:
 * <ul>
 *     <li>element główny {@code <gpx>} nie deklaruje przestrzeni nazw ani atrybutów
 *         {@code version} i {@code creator} — parser porównuje więc wyłącznie nazwy lokalne,</li>
 *     <li>znaczniki {@code <time>} wewnątrz {@code <trkpt>} nie zawierają oznaczenia strefy
 *         czasowej — zgodnie ze specyfikacją GPX czas jest wtedy interpretowany jako UTC,</li>
 *     <li>{@code <metadata><time>} bywa artefaktem zerowego tygodnia GPS (rok 1980) i jest pomijany.</li>
 * </ul>
 * Parser jest odporny na atak XXE (obsługa DTD oraz encji zewnętrznych jest wyłączona),
 * ponieważ przetwarza pliki pochodzące od użytkowników.
 */
@Slf4j
@Component
public class GpxParser {

    /** Twardy limit liczby punktów chroniący serwer przed wyczerpaniem pamięci. */
    public static final int MAX_POINTS = 200_000;

    /** Numer obrączki zapisywany przez Skyleader jako "Ring-8414". */
    private static final Pattern RING_PATTERN = Pattern.compile("(?i)^\\s*ring[\\s\\-_:]*([A-Za-z0-9\\-/]+)\\s*$");

    private final XMLInputFactory inputFactory;

    public GpxParser() {
        this.inputFactory = XMLInputFactory.newInstance();
        this.inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        this.inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        this.inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    /**
     * Odczytuje trasę z pliku GPX.
     *
     * @throws GpxParsingException gdy plik nie jest poprawnym XML-em lub nie zawiera punktów trasy
     */
    public GpxTrack parse(InputStream inputStream) {
        List<GpxPoint> points = new ArrayList<>();
        String trackName = null;
        String creator = null;

        XMLStreamReader reader = null;
        try {
            reader = inputFactory.createXMLStreamReader(inputStream);

            boolean insideTrack = false;
            boolean insideMetadata = false;
            Double latitude = null;
            Double longitude = null;
            Double elevation = null;
            Instant time = null;
            boolean insidePoint = false;

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String element = reader.getLocalName();

                    switch (element) {
                        case "gpx" -> creator = reader.getAttributeValue(null, "creator");
                        case "metadata" -> insideMetadata = true;
                        case "trk" -> insideTrack = true;
                        case "name" -> {
                            if (insideTrack && trackName == null) {
                                trackName = reader.getElementText().trim();
                            }
                        }
                        case "text" -> {
                            if (insideMetadata && creator == null) {
                                creator = reader.getElementText().trim();
                            }
                        }
                        case "trkpt" -> {
                            insidePoint = true;
                            elevation = null;
                            time = null;
                            latitude = parseCoordinate(reader.getAttributeValue(null, "lat"));
                            longitude = parseCoordinate(reader.getAttributeValue(null, "lon"));
                        }
                        case "ele" -> {
                            if (insidePoint) {
                                elevation = parseDouble(reader.getElementText());
                            }
                        }
                        case "time" -> {
                            if (insidePoint) {
                                time = parseTime(reader.getElementText());
                            }
                        }
                        default -> {
                            // pozostałe znaczniki (np. <extensions>, <sat>, <hdop>) są pomijane
                        }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String element = reader.getLocalName();

                    if ("trkpt".equals(element)) {
                        insidePoint = false;
                        if (latitude != null && longitude != null && isOnEarth(latitude, longitude)) {
                            if (points.size() >= MAX_POINTS) {
                                throw new GpxParsingException(
                                        "Plik zawiera zbyt wiele punktów trasy (limit: " + MAX_POINTS + ").");
                            }
                            points.add(new GpxPoint(latitude, longitude, elevation, time));
                        }
                        latitude = null;
                        longitude = null;
                    } else if ("metadata".equals(element)) {
                        insideMetadata = false;
                    } else if ("trk".equals(element)) {
                        insideTrack = false;
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new GpxParsingException("Nie udało się odczytać pliku GPX — plik jest uszkodzony "
                    + "lub nie jest poprawnym dokumentem XML.", e);
        } finally {
            closeQuietly(reader);
        }

        if (points.isEmpty()) {
            throw new GpxParsingException("Plik GPX nie zawiera żadnych punktów trasy (<trkpt>).");
        }

        return new GpxTrack(trackName, extractRingNumber(trackName), creator, List.copyOf(points));
    }

    /**
     * Wyłuskuje numer obrączki z nazwy trasy. Skyleader zapisuje ją w formacie "Ring-8414".
     */
    private String extractRingNumber(String trackName) {
        if (trackName == null || trackName.isBlank()) {
            return null;
        }
        Matcher matcher = RING_PATTERN.matcher(trackName);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return trackName.length() <= 32 ? trackName.trim() : null;
    }

    /**
     * Odczytuje znacznik czasu. Zgodnie ze specyfikacją GPX czas bez oznaczenia strefy
     * traktowany jest jako UTC — tak właśnie zapisuje go oprogramowanie Skyleader.
     */
    private Instant parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // czas bez strefy, np. "2026-08-30T05:20:54"
        }
        try {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            log.debug("Pominięto nieczytelny znacznik czasu w pliku GPX: {}", value);
            return null;
        }
    }

    private Double parseCoordinate(String raw) {
        Double value = parseDouble(raw);
        return value != null && Double.isFinite(value) ? value : null;
    }

    private Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isOnEarth(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }

    private void closeQuietly(XMLStreamReader reader) {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (XMLStreamException e) {
            log.debug("Nie udało się zamknąć czytnika XML: {}", e.getMessage());
        }
    }
}
