package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.*;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.FlightTrackPoint;
import com.pzhgp.backend.entity.PigeonFlight;
import com.pzhgp.backend.entity.Role;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FlightTrackPointRepository;
import com.pzhgp.backend.repository.PigeonFlightRepository;
import com.pzhgp.backend.service.gpx.*;
import com.pzhgp.backend.utils.PaginationUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Obsługa modułu "Mapy lotów": wgrywanie plików GPX, analiza trasy i udostępnianie wyników.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PigeonFlightService {

    /** Maksymalny rozmiar wgrywanego pliku GPX w bajtach. */
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    /** Maksymalna liczba lotów przypadająca na jednego hodowcę. */
    private static final long MAX_FLIGHTS_PER_BREEDER = 200;

    /** Okno czasowe wygładzania prędkości chwilowej, w sekundach. */
    private static final long SPEED_SMOOTHING_WINDOW_SECONDS = 10;

    /** Domyślna tolerancja upraszczania trasy dla widoku mapy, w metrach. */
    private static final double DEFAULT_SIMPLIFY_TOLERANCE_METERS = 10.0;

    private final PigeonFlightRepository flightRepository;
    private final FlightTrackPointRepository trackPointRepository;
    private final BreederRepository breederRepository;
    private final GpxParser gpxParser;
    private final FlightAnalyzer flightAnalyzer;

    /**
     * Wgrywa plik GPX, analizuje trasę i zapisuje lot wraz z punktami trasy.
     *
     * @return identyfikator utworzonego lotu
     */
    @Transactional
    public Long uploadFlight(MultipartFile file, FlightUploadRequest request, String userEmail) {
        Breeder owner = requireBreeder(userEmail);
        validateFile(file);


        if (flightRepository.countByOwner(owner) >= MAX_FLIGHTS_PER_BREEDER) {
            throw new IllegalStateException("Osiągnięto limit " + MAX_FLIGHTS_PER_BREEDER
                    + " zapisanych lotów. Usuń starsze loty, aby wgrać kolejny.");
        }

        GpxTrack track;
        try (InputStream inputStream = file.getInputStream()) {
            track = gpxParser.parse(inputStream);
        } catch (IOException e) {
            throw new GpxParsingException("Nie udało się odczytać przesłanego pliku.", e);
        }

        FlightAnalysis analysis = flightAnalyzer.analyze(track.points());

        PigeonFlight flight = new PigeonFlight();
        flight.setOwner(owner);
        flight.setName(resolveName(request, track, file));
        flight.setRingNumber(resolveRingNumber(request, track));
        flight.setReleaseSite(trimToNull(request != null ? request.releaseSite() : null));
        flight.setOriginalFileName(sanitizeFileName(file.getOriginalFilename()));
        applyAnalysis(flight, analysis);

        List<Double> speeds = computeSmoothedSpeeds(track.points());
        for (int i = 0; i < track.points().size(); i++) {
            GpxPoint point = track.points().get(i);
            flight.addTrackPoint(new FlightTrackPoint(
                    i, point.latitude(), point.longitude(), point.elevation(), point.time(), speeds.get(i)));
        }

        PigeonFlight saved = flightRepository.save(flight);

        log.info("Hodowca {} wgrał lot '{}' ({} punktów, dystans {} km, prędkość {} m/min).",
                userEmail, saved.getName(), analysis.totalPoints(),
                Math.round(analysis.straightLineDistanceMeters() / 1000),
                Math.round(analysis.averageSpeedMetersPerMinute()));

        return saved.getId();
    }

    /**
     * Lista lotów zalogowanego hodowcy, od najnowszego.
     */
    @Transactional(readOnly = true)
    public Page<FlightSummaryDto> getMyFlights(String userEmail, int page, int size) {
        Breeder owner = requireBreeder(userEmail);
        PaginationUtils.validate(page, size, 50);
        Pageable pageable = PageRequest.of(page, size);

        return flightRepository.findByOwnerOrderByUploadedAtDesc(owner, pageable)
                .map(flight -> toSummary(flight, owner));
    }

    /**
     * Szczegóły lotu wraz z trasą.
     *
     * @param simplifyToleranceMeters tolerancja upraszczania trasy w metrach;
     *                                {@code null} oznacza wartość domyślną, {@code 0} — brak upraszczania
     */
    @Transactional(readOnly = true)
    public FlightDetailsDto getFlightDetails(Long flightId, String userEmail, Double simplifyToleranceMeters) {
        Breeder viewer = requireBreeder(userEmail);
        PigeonFlight flight = flightRepository.findWithOwnerById(flightId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono lotu o ID: " + flightId));

        requireAccess(flight, viewer);

        List<FlightTrackPoint> storedPoints = trackPointRepository.findByFlightIdOrderByPointIndexAsc(flightId);
        double tolerance = simplifyToleranceMeters != null
                ? Math.max(0, simplifyToleranceMeters) : DEFAULT_SIMPLIFY_TOLERANCE_METERS;

        List<FlightTrackPointDto> trackPoints = simplifyForDisplay(storedPoints, tolerance);

        return new FlightDetailsDto(
                flight.getId(),
                flight.getName(),
                flight.getRingNumber(),
                flight.getReleaseSite(),
                fullName(flight.getOwner()),
                flight.getOriginalFileName(),
                flight.getStartTime(),
                flight.getEndTime(),
                flight.getStartLatitude(),
                flight.getStartLongitude(),
                flight.getEndLatitude(),
                flight.getEndLongitude(),
                toStatistics(flight),
                trackPoints,
                trackPoints.size(),
                flight.getUploadedAt(),
                canDelete(flight, viewer)
        );
    }

    /**
     * Usuwa lot wraz z punktami trasy. Uprawniony jest właściciel lotu oraz administrator.
     */
    @Transactional
    public void deleteFlight(Long flightId, String userEmail) {
        Breeder viewer = requireBreeder(userEmail);
        PigeonFlight flight = flightRepository.findWithOwnerById(flightId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono lotu o ID: " + flightId));

        if (!canDelete(flight, viewer)) {
            throw new IllegalStateException("Brak uprawnień do usunięcia tego lotu.");
        }

        flightRepository.delete(flight);
    }

    /**
     * Przepisuje wynik analizy na pola encji lotu.
     */
    private void applyAnalysis(PigeonFlight flight, FlightAnalysis analysis) {
        flight.setStartTime(analysis.startTime());
        flight.setEndTime(analysis.endTime());
        flight.setStartLatitude(analysis.startLatitude());
        flight.setStartLongitude(analysis.startLongitude());
        flight.setEndLatitude(analysis.endLatitude());
        flight.setEndLongitude(analysis.endLongitude());
        flight.setStraightLineDistanceMeters(analysis.straightLineDistanceMeters());
        flight.setTrackDistanceMeters(analysis.trackDistanceMeters());
        flight.setDurationSeconds(analysis.durationSeconds());
        flight.setAverageSpeedMetersPerMinute(analysis.averageSpeedMetersPerMinute());
        flight.setStraightLineSpeedMetersPerMinute(analysis.straightLineSpeedMetersPerMinute());
        flight.setMaxSpeedMetersPerMinute(analysis.maxSpeedMetersPerMinute());
        flight.setMinElevationMeters(analysis.minElevationMeters());
        flight.setMaxElevationMeters(analysis.maxElevationMeters());
        flight.setElevationGainMeters(analysis.elevationGainMeters());
        flight.setTimestampsAvailable(analysis.timestampsAvailable());
        flight.setTotalPoints(analysis.totalPoints());
    }

    /**
     * Prędkość chwilowa w każdym punkcie, uśredniona po oknie czasowym rozciągniętym
     * symetrycznie wokół punktu. Bez uśredniania prędkość liczona z sąsiednich odczytów
     * co 5 s jest zdominowana przez błąd pozycji.
     */
    private List<Double> computeSmoothedSpeeds(List<GpxPoint> points) {
        List<Double> speeds = new ArrayList<>(points.size());

        for (int i = 0; i < points.size(); i++) {
            speeds.add(smoothedSpeedAt(points, i));
        }
        return speeds;
    }

    private Double smoothedSpeedAt(List<GpxPoint> points, int index) {
        if (points.get(index).time() == null) {
            return null;
        }

        int left = index;
        int right = index;

        while (true) {
            GpxPoint from = points.get(left);
            GpxPoint to = points.get(right);
            if (from.time() != null && to.time() != null
                    && Duration.between(from.time(), to.time()).getSeconds() >= SPEED_SMOOTHING_WINDOW_SECONDS) {
                break;
            }
            boolean canExtendLeft = left > 0;
            boolean canExtendRight = right < points.size() - 1;
            if (!canExtendLeft && !canExtendRight) {
                break;
            }
            if (canExtendLeft) {
                left--;
            }
            if (canExtendRight) {
                right++;
            }
        }

        GpxPoint from = points.get(left);
        GpxPoint to = points.get(right);
        if (from.time() == null || to.time() == null) {
            return null;
        }

        long seconds = Duration.between(from.time(), to.time()).getSeconds();
        if (seconds <= 0) {
            return null;
        }

        double distance = 0.0;
        for (int i = left + 1; i <= right; i++) {
            GpxPoint previous = points.get(i - 1);
            GpxPoint current = points.get(i);
            distance += GeoMath.distance(
                    previous.latitude(), previous.longitude(), current.latitude(), current.longitude());
        }

        return round(distance / (seconds / 60.0), 1);
    }

    /**
     * Upraszcza zapisaną trasę przed wysłaniem do przeglądarki. Algorytm zwraca indeksy
     * zachowanych punktów, dzięki czemu wyliczone wcześniej prędkości chwilowe i wysokości
     * trafiają do odpowiedzi bez ponownego przeliczania.
     */
    private List<FlightTrackPointDto> simplifyForDisplay(List<FlightTrackPoint> storedPoints, double tolerance) {
        List<GpxPoint> asGpxPoints = storedPoints.stream()
                .map(p -> new GpxPoint(p.getLatitude(), p.getLongitude(), p.getElevationMeters(), p.getRecordedAt()))
                .toList();

        return TrackSimplifier.selectIndices(asGpxPoints, tolerance).stream()
                .map(storedPoints::get)
                .map(stored -> new FlightTrackPointDto(
                        stored.getLatitude(),
                        stored.getLongitude(),
                        stored.getElevationMeters(),
                        stored.getRecordedAt(),
                        stored.getSpeedMetersPerMinute()))
                .toList();
    }

    private FlightSummaryDto toSummary(PigeonFlight flight, Breeder viewer) {
        return new FlightSummaryDto(
                flight.getId(),
                flight.getName(),
                flight.getRingNumber(),
                flight.getReleaseSite(),
                fullName(flight.getOwner()),
                flight.getStartTime(),
                flight.getEndTime(),
                round(flight.getStraightLineDistanceMeters() / 1000.0, 2),
                flight.getDurationSeconds(),
                round(flight.getAverageSpeedMetersPerMinute(), 0),
                flight.getTotalPoints(),
                flight.isTimestampsAvailable(),
                flight.getUploadedAt(),
                canDelete(flight, viewer)
        );
    }

    private FlightStatisticsDto toStatistics(PigeonFlight flight) {
        return new FlightStatisticsDto(
                round(flight.getStraightLineDistanceMeters() / 1000.0, 2),
                round(flight.getTrackDistanceMeters() / 1000.0, 2),
                flight.getDurationSeconds(),
                round(flight.getAverageSpeedMetersPerMinute(), 0),
                round(flight.getStraightLineSpeedMetersPerMinute(), 0),
                round(flight.getMaxSpeedMetersPerMinute(), 0),
                round(flight.getMinElevationMeters(), 0),
                round(flight.getMaxElevationMeters(), 0),
                round(flight.getElevationGainMeters(), 0),
                flight.getTotalPoints(),
                flight.isTimestampsAvailable()
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Nie wybrano pliku GPX.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Plik jest za duży. Maksymalny rozmiar to "
                    + (MAX_FILE_SIZE_BYTES / (1024 * 1024)) + " MB.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".gpx")) {
            throw new IllegalArgumentException("Dozwolone są wyłącznie pliki z rozszerzeniem .gpx.");
        }
    }

    private String resolveName(FlightUploadRequest request, GpxTrack track, MultipartFile file) {
        String provided = trimToNull(request != null ? request.name() : null);
        if (provided != null) {
            return provided;
        }
        String fileName = sanitizeFileName(file.getOriginalFilename());
        String withoutExtension = fileName.replaceFirst("(?i)\\.gpx$", "");

        String fallback = trimToNull(withoutExtension) != null ? withoutExtension : track.trackName();
        if (fallback == null || fallback.isBlank()) {
            fallback = "Lot gołębia";
        }
        return fallback.length() > 150 ? fallback.substring(0, 150) : fallback;
    }

    private String resolveRingNumber(FlightUploadRequest request, GpxTrack track) {
        String provided = trimToNull(request != null ? request.ringNumber() : null);
        if (provided != null) {
            return provided;
        }
        String fromFile = track.ringNumber();
        if (fromFile == null) {
            return null;
        }
        return fromFile.length() > 32 ? fromFile.substring(0, 32) : fromFile;
    }

    /**
     * Usuwa z nazwy pliku elementy ścieżki, aby nazwa pochodząca od użytkownika
     * nie mogła wskazywać na inny katalog.
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "trasa.gpx";
        }
        String bare = fileName.replaceAll(".*[/\\\\]", "").trim();
        if (bare.isBlank()) {
            return "trasa.gpx";
        }
        return bare.length() > 255 ? bare.substring(bare.length() - 255) : bare;
    }

    private Breeder requireBreeder(String email) {
        return breederRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono konta hodowcy."));
    }

    private void requireAccess(PigeonFlight flight, Breeder viewer) {
        boolean isOwner = flight.getOwner().getId().equals(viewer.getId());
        if (!isOwner && viewer.getRole() != Role.ADMINISTRATOR) {
            throw new IllegalStateException("Brak uprawnień do przeglądania tego lotu.");
        }
    }

    private boolean canDelete(PigeonFlight flight, Breeder viewer) {
        return flight.getOwner().getId().equals(viewer.getId()) || viewer.getRole() == Role.ADMINISTRATOR;
    }

    private String fullName(Breeder breeder) {
        return breeder.getName() + " " + breeder.getSurname();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    private Double round(Double value, int decimals) {
        return value == null ? null : round(value.doubleValue(), decimals);
    }
}
