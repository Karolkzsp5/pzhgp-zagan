package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.FlightDetailsDto;
import com.pzhgp.backend.dto.FlightSummaryDto;
import com.pzhgp.backend.dto.FlightUploadRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FlightTrackPointRepository;
import com.pzhgp.backend.repository.PigeonFlightRepository;
import com.pzhgp.backend.service.gpx.FlightAnalyzer;
import com.pzhgp.backend.service.gpx.GpxParser;
import com.pzhgp.backend.service.gpx.GpxParsingException;
import com.pzhgp.backend.service.gpx.GpxPoint;
import com.pzhgp.backend.service.gpx.GpxTrack;
import com.pzhgp.backend.exception.ForbiddenOperationException;
import com.pzhgp.backend.exception.ResourceConflictException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PigeonFlightServiceTest {

    @Mock
    private PigeonFlightRepository flightRepository;

    @Mock
    private FlightTrackPointRepository trackPointRepository;

    @Mock
    private BreederRepository breederRepository;

    @Spy
    private GpxParser gpxParser = new GpxParser();

    @Spy
    private FlightAnalyzer flightAnalyzer = new FlightAnalyzer();

    @InjectMocks
    private PigeonFlightService pigeonFlightService;

    private Breeder owner;
    private Breeder otherBreeder;
    private Breeder administrator;

    @BeforeEach
    void setUp() {
        Section section = new Section();
        section.setId(1L);
        section.setName("Żagań");

        owner = breeder(1L, "Jan", "Kowalski", "jan@example.com", Role.BREEDER, section);
        otherBreeder = breeder(2L, "Adam", "Nowak", "adam@example.com", Role.BREEDER, section);
        administrator = breeder(3L, "Piotr", "Admin", "admin@example.com", Role.ADMINISTRATOR, section);
    }

    @Test
    @DisplayName("Should save an uploaded GPX file with its track points and calculated statistics")
    void savesFlightWithTrackPoints() throws IOException {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.countByOwner(owner)).thenReturn(0L);
        when(flightRepository.save(any(PigeonFlight.class))).thenAnswer(invocation -> {
            PigeonFlight saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        Long id = pigeonFlightService.uploadFlight(realGpxFile(), null, "jan@example.com");

        assertEquals(42L, id);

        ArgumentCaptor<PigeonFlight> captor = ArgumentCaptor.forClass(PigeonFlight.class);
        verify(flightRepository).save(captor.capture());

        PigeonFlight saved = captor.getValue();

        assertEquals(owner, saved.getOwner());
        assertEquals(1230, saved.getTotalPoints());
        assertEquals(1230, saved.getTrackPoints().size());
        assertEquals(184.4, saved.getStraightLineDistanceMeters() / 1000.0, 1.0);
        assertEquals(638.0, saved.getAverageSpeedMetersPerMinute(), 20.0);
        assertTrue(saved.isTimestampsAvailable());
        assertSame(saved, saved.getTrackPoints().getFirst().getFlight());
    }

    @Test
    @DisplayName("Should use flight name and ring number from the GPX file when the breeder omits them")
    void fallsBackToFileMetadata() throws IOException {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.countByOwner(owner)).thenReturn(0L);
        when(flightRepository.save(any(PigeonFlight.class))).thenAnswer(i -> i.getArgument(0));

        pigeonFlightService.uploadFlight(realGpxFile(), null, "jan@example.com");

        ArgumentCaptor<PigeonFlight> captor = ArgumentCaptor.forClass(PigeonFlight.class);
        verify(flightRepository).save(captor.capture());

        assertEquals("8414", captor.getValue().getRingNumber());
        assertEquals("Lot_konkursowy_30.08.2026", captor.getValue().getName());
    }

    @Test
    @DisplayName("Should prefer user-provided metadata over metadata from the GPX file")
    void prefersUserProvidedMetadata() throws IOException {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.countByOwner(owner)).thenReturn(0L);
        when(flightRepository.save(any(PigeonFlight.class))).thenAnswer(i -> i.getArgument(0));

        FlightUploadRequest request = new FlightUploadRequest("Lot z Dessau", "PL-0208-24-1234", "Dessau");
        pigeonFlightService.uploadFlight(realGpxFile(), request, "jan@example.com");

        ArgumentCaptor<PigeonFlight> captor = ArgumentCaptor.forClass(PigeonFlight.class);
        verify(flightRepository).save(captor.capture());

        assertEquals("Lot z Dessau", captor.getValue().getName());
        assertEquals("PL-0208-24-1234", captor.getValue().getRingNumber());
        assertEquals("Dessau", captor.getValue().getReleaseSite());
    }

    @Test
    @DisplayName("Should treat blank user metadata as missing and fall back where applicable")
    void trimsOptionalMetadataToNull() throws IOException {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.countByOwner(owner)).thenReturn(0L);
        when(flightRepository.save(any(PigeonFlight.class))).thenAnswer(i -> i.getArgument(0));

        FlightUploadRequest request = new FlightUploadRequest("   ", "   ", "   ");
        pigeonFlightService.uploadFlight(realGpxFile(), request, "jan@example.com");

        ArgumentCaptor<PigeonFlight> captor = ArgumentCaptor.forClass(PigeonFlight.class);
        verify(flightRepository).save(captor.capture());

        PigeonFlight saved = captor.getValue();

        assertEquals("Lot_konkursowy_30.08.2026", saved.getName());
        assertEquals("8414", saved.getRingNumber());
        assertNull(saved.getReleaseSite());
    }

    @Test
    @DisplayName("Should remove path elements from the original uploaded file name")
    void sanitizesFileName() throws IOException {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.countByOwner(owner)).thenReturn(0L);
        when(flightRepository.save(any(PigeonFlight.class))).thenAnswer(i -> i.getArgument(0));

        try (InputStream stream = getClass().getResourceAsStream("/gpx/skyleader-minimalny.gpx")) {
            assertNotNull(stream);

            MockMultipartFile file = new MockMultipartFile("file", "../../../etc/malicious.gpx",
                    "application/gpx+xml", stream.readAllBytes());

            pigeonFlightService.uploadFlight(file, null, "jan@example.com");
        }

        ArgumentCaptor<PigeonFlight> captor = ArgumentCaptor.forClass(PigeonFlight.class);
        verify(flightRepository).save(captor.capture());

        assertEquals("malicious.gpx", captor.getValue().getOriginalFileName());
    }

    @Test
    @DisplayName("Should calculate and store smoothed speeds for track points with timestamps")
    void storesSmoothedPointSpeeds() {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.countByOwner(owner)).thenReturn(0L);
        when(flightRepository.save(any(PigeonFlight.class))).thenAnswer(invocation -> {
            PigeonFlight saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        Instant base = Instant.parse("2026-08-30T05:00:00Z");
        List<GpxPoint> points = List.of(
                new GpxPoint(51.0000000, 15.0, 100.0, base),
                new GpxPoint(51.00089932, 15.0, 100.0, base.plusSeconds(5)),
                new GpxPoint(51.00179864, 15.0, 100.0, base.plusSeconds(10)));

        GpxTrack track = new GpxTrack("Ring-1234", "1234", "Test", points);
        doReturn(track).when(gpxParser).parse(any(InputStream.class));

        MockMultipartFile file = new MockMultipartFile("file", "speed-test.gpx",
                "application/gpx+xml", "<gpx/>".getBytes(StandardCharsets.UTF_8));

        pigeonFlightService.uploadFlight(file, null, "jan@example.com");

        ArgumentCaptor<PigeonFlight> captor = ArgumentCaptor.forClass(PigeonFlight.class);
        verify(flightRepository).save(captor.capture());

        List<FlightTrackPoint> storedPoints = captor.getValue().getTrackPoints();

        assertEquals(3, storedPoints.size());
        for (FlightTrackPoint point : storedPoints) {
            assertNotNull(point.getSpeedMetersPerMinute());
            assertEquals(1200.0, point.getSpeedMetersPerMinute(), 15.0);
        }
    }

    @Test
    @DisplayName("Should store null instantaneous speeds for track points without timestamps")
    void storesNullPointSpeedsWithoutTimestamps() {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.countByOwner(owner)).thenReturn(0L);
        when(flightRepository.save(any(PigeonFlight.class))).thenAnswer(invocation -> {
            PigeonFlight saved = invocation.getArgument(0);
            saved.setId(6L);
            return saved;
        });

        String gpx = """
                <gpx>
                    <trk>
                        <trkseg>
                            <trkpt lat="51.0000" lon="15.0000"/>
                            <trkpt lat="51.0010" lon="15.0000"/>
                            <trkpt lat="51.0020" lon="15.0000"/>
                        </trkseg>
                    </trk>
                </gpx>
                """;

        MockMultipartFile file = new MockMultipartFile("file", "without-time.gpx",
                "application/gpx+xml", gpx.getBytes(StandardCharsets.UTF_8));

        pigeonFlightService.uploadFlight(file, null, "jan@example.com");

        ArgumentCaptor<PigeonFlight> captor = ArgumentCaptor.forClass(PigeonFlight.class);
        verify(flightRepository).save(captor.capture());

        assertFalse(captor.getValue().isTimestampsAvailable());
        assertTrue(captor.getValue().getTrackPoints().stream().allMatch(point -> point.getSpeedMetersPerMinute() == null));
    }

    @Test
    @DisplayName("Should preserve all points for non-positive tolerance and simplify routes otherwise")
    void appliesSimplificationTolerance() {
        PigeonFlight flight = flightOwnedBy(owner);

        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.findWithOwnerById(7L)).thenReturn(Optional.of(flight));

        List<FlightTrackPoint> collinear = java.util.stream.IntStream.range(0, 30)
                .mapToObj(i -> trackPoint(i, 51.0 + i * 0.001, 15.0))
                .toList();

        when(trackPointRepository.findByFlightIdOrderByPointIndexAsc(7L)).thenReturn(collinear);

        FlightDetailsDto zero = pigeonFlightService.getFlightDetails(7L, "jan@example.com", 0.0);
        FlightDetailsDto negative = pigeonFlightService.getFlightDetails(7L, "jan@example.com", -10.0);
        FlightDetailsDto positive = pigeonFlightService.getFlightDetails(7L, "jan@example.com", 10.0);
        FlightDetailsDto defaultTolerance = pigeonFlightService.getFlightDetails(7L, "jan@example.com", null);

        assertEquals(30, zero.returnedPoints());
        assertEquals(30, negative.returnedPoints());
        assertEquals(2, positive.returnedPoints());
        assertEquals(2, defaultTolerance.returnedPoints());
        assertTrue(zero.canDelete());
    }

    @Test
    @DisplayName("Should reject a file with an extension other than GPX")
    void rejectsNonGpxFile() {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));

        MockMultipartFile file = new MockMultipartFile("file", "track.txt",
                "text/plain", "not a GPX file".getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> pigeonFlightService.uploadFlight(file, null, "jan@example.com"));

        assertTrue(exception.getMessage().contains(".gpx"));
        verify(flightRepository, never()).countByOwner(any());
        verify(flightRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject an empty GPX file")
    void rejectsEmptyFile() {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        MockMultipartFile file = new MockMultipartFile("file", "track.gpx", "application/gpx+xml", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> pigeonFlightService.uploadFlight(file, null, "jan@example.com"));
        verify(flightRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject a GPX file larger than 10 MB")
    void rejectsOversizedFile() {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L * 1024 * 1024 + 1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> pigeonFlightService.uploadFlight(file, null, "jan@example.com"));

        assertTrue(exception.getMessage().contains("10 MB"));
        verify(flightRepository, never()).countByOwner(any());
        verify(flightRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw GpxParsingException for malformed GPX content")
    void rejectsGarbageContent() {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.countByOwner(owner)).thenReturn(0L);

        MockMultipartFile file = new MockMultipartFile("file", "track.gpx",
                "application/gpx+xml", "this is not XML".getBytes(StandardCharsets.UTF_8));

        assertThrows(GpxParsingException.class, () -> pigeonFlightService.uploadFlight(file, null, "jan@example.com"));
        verify(flightRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should convert IOException to GpxParsingException when reading an uploaded file")
    void convertsIoFailureToGpxParsingException() throws IOException {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.countByOwner(owner)).thenReturn(0L);

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getOriginalFilename()).thenReturn("track.gpx");
        when(file.getInputStream()).thenThrow(new IOException("Disk read error"));

        GpxParsingException exception = assertThrows(GpxParsingException.class,
                () -> pigeonFlightService.uploadFlight(file, null, "jan@example.com"));

        assertNotNull(exception.getCause());
        assertInstanceOf(IOException.class, exception.getCause());
        verify(flightRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should block another flight upload when the breeder reaches the flight limit")
    void enforcesFlightLimit() throws IOException {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.countByOwner(owner)).thenReturn(200L);

        ResourceConflictException exception = assertThrows(ResourceConflictException.class,
                () -> pigeonFlightService.uploadFlight(realGpxFile(), null, "jan@example.com"));

        assertTrue(exception.getMessage().contains("limit"));
        verify(flightRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject an unknown breeder before processing the GPX file")
    void rejectsUnknownBreeder() {
        when(breederRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> pigeonFlightService.uploadFlight(null, null, "missing@example.com"));

        verifyNoInteractions(flightRepository);
        verifyNoInteractions(trackPointRepository);
    }

    @Test
    @DisplayName("Should map flight summaries and cap the requested page size at 50")
    void mapsFlightListAndCapsPageSize() {
        PigeonFlight flight = completeFlightOwnedBy(owner);

        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.findByOwnerOrderByUploadedAtDescIdDesc(eq(owner), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(flight)));

        Page<FlightSummaryDto> result = pigeonFlightService.getMyFlights("jan@example.com", 2, 50);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(flightRepository).findByOwnerOrderByUploadedAtDescIdDesc(eq(owner), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(2, pageable.getPageNumber());
        assertEquals(50, pageable.getPageSize());
        assertEquals(1, result.getContent().size());

        FlightSummaryDto summary = result.getContent().getFirst();

        assertEquals(7L, summary.id());
        assertEquals("Test flight", summary.name());
        assertEquals("1234", summary.ringNumber());
        assertEquals("Dessau", summary.releaseSite());
        assertEquals("Jan Kowalski", summary.ownerName());
        assertEquals(12.35, summary.straightLineDistanceKm(), 0.001);
        assertEquals(1235.0, summary.averageSpeedMetersPerMinute());
        assertEquals(120, summary.totalPoints());
        assertTrue(summary.timestampsAvailable());
        assertTrue(summary.canDelete());
    }

    @Test
    @DisplayName("Should reject negative page numbers and zero page sizes")
    void rejectsInvalidPaginationArguments() {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));

        assertThrows(IllegalArgumentException.class, () -> pigeonFlightService.getMyFlights("jan@example.com", -1, 10));
        assertThrows(IllegalArgumentException.class, () -> pigeonFlightService.getMyFlights("jan@example.com", 0, 0));

        verify(flightRepository, never()).findByOwnerOrderByUploadedAtDescIdDesc(any(), any());
    }

    @Test
    @DisplayName("Should forbid a breeder from viewing another breeder's flight")
    void deniesAccessToForeignFlight() {
        PigeonFlight flight = flightOwnedBy(owner);

        when(breederRepository.findByEmail("adam@example.com")).thenReturn(Optional.of(otherBreeder));
        when(flightRepository.findWithOwnerById(7L)).thenReturn(Optional.of(flight));

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> pigeonFlightService.getFlightDetails(7L, "adam@example.com", null));

        assertTrue(exception.getMessage().contains("uprawnień"));
        verifyNoInteractions(trackPointRepository);
    }

    @Test
    @DisplayName("Should allow an administrator to view flights owned by other breeders")
    void allowsAdministratorToViewAnyFlight() {
        PigeonFlight flight = completeFlightOwnedBy(owner);

        when(breederRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(administrator));
        when(flightRepository.findWithOwnerById(7L)).thenReturn(Optional.of(flight));
        when(trackPointRepository.findByFlightIdOrderByPointIndexAsc(7L)).thenReturn(List.of(
                trackPoint(0, 51.0, 15.0),
                trackPoint(1, 51.1, 15.1),
                trackPoint(2, 51.2, 15.2)));

        FlightDetailsDto details = pigeonFlightService.getFlightDetails(7L, "admin@example.com", 0.0);

        assertEquals("Jan Kowalski", details.ownerName());
        assertTrue(details.canDelete());
        assertEquals(12.35, details.statistics().straightLineDistanceKm(), 0.001);
        assertEquals(13.0, details.statistics().trackDistanceKm(), 0.001);
        assertEquals(1235.0, details.statistics().averageSpeedMetersPerMinute());
        assertEquals(3, details.returnedPoints());
        assertEquals(80.0, details.trackPoints().getFirst().speedMetersPerMinute(), 0.001);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when a flight does not exist")
    void reportsMissingFlight() {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.findWithOwnerById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> pigeonFlightService.getFlightDetails(999L, "jan@example.com", null));
    }

    @Test
    @DisplayName("Should allow a flight owner to delete their own flight")
    void allowsOwnerToDeleteFlight() {
        PigeonFlight flight = flightOwnedBy(owner);

        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.findWithOwnerById(7L)).thenReturn(Optional.of(flight));

        pigeonFlightService.deleteFlight(7L, "jan@example.com");
        verify(flightRepository).delete(flight);
    }

    @Test
    @DisplayName("Should forbid a breeder from deleting another breeder's flight but allow an administrator")
    void enforcesDeletePermissions() {
        PigeonFlight flight = flightOwnedBy(owner);

        when(flightRepository.findWithOwnerById(7L)).thenReturn(Optional.of(flight));
        when(breederRepository.findByEmail("adam@example.com")).thenReturn(Optional.of(otherBreeder));

        assertThrows(ForbiddenOperationException.class, () -> pigeonFlightService.deleteFlight(7L, "adam@example.com"));
        verify(flightRepository, never()).delete(any());

        when(breederRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(administrator));
        pigeonFlightService.deleteFlight(7L, "admin@example.com");
        verify(flightRepository).delete(flight);
    }

    private Breeder breeder(Long id, String name, String surname, String email, Role role, Section section) {
        Breeder breeder = new Breeder();
        breeder.setId(id);
        breeder.setName(name);
        breeder.setSurname(surname);
        breeder.setEmail(email);
        breeder.setRole(role);
        breeder.setSection(section);
        breeder.setStatus(AccountStatus.ACTIVE);
        return breeder;
    }

    private MockMultipartFile realGpxFile() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/gpx/skyleader-lot-konkursowy.gpx")) {
            assertNotNull(stream);
            return new MockMultipartFile("file", "Lot_konkursowy_30.08.2026.gpx",
                    "application/gpx+xml", stream.readAllBytes());
        }
    }

    private PigeonFlight flightOwnedBy(Breeder breeder) {
        PigeonFlight flight = new PigeonFlight();
        flight.setId(7L);
        flight.setOwner(breeder);
        flight.setName("Test flight");
        flight.setOriginalFileName("test.gpx");
        return flight;
    }

    private PigeonFlight completeFlightOwnedBy(Breeder breeder) {
        PigeonFlight flight = flightOwnedBy(breeder);

        flight.setRingNumber("1234");
        flight.setReleaseSite("Dessau");
        flight.setStartTime(Instant.parse("2026-08-30T05:00:00Z"));
        flight.setEndTime(Instant.parse("2026-08-30T05:10:00Z"));
        flight.setStartLatitude(51.0);
        flight.setStartLongitude(15.0);
        flight.setEndLatitude(51.1);
        flight.setEndLongitude(15.1);
        flight.setStraightLineDistanceMeters(12_345.6);
        flight.setTrackDistanceMeters(13_000.0);
        flight.setDurationSeconds(600);
        flight.setAverageSpeedMetersPerMinute(1234.6);
        flight.setStraightLineSpeedMetersPerMinute(1200.4);
        flight.setMaxSpeedMetersPerMinute(1600.7);
        flight.setMinElevationMeters(100.2);
        flight.setMaxElevationMeters(250.7);
        flight.setElevationGainMeters(300.4);
        flight.setTimestampsAvailable(true);
        flight.setTotalPoints(120);
        flight.setUploadedAt(Instant.parse("2026-08-30T10:00:00Z"));

        return flight;
    }

    private FlightTrackPoint trackPoint(int index, double latitude, double longitude) {
        return new FlightTrackPoint(index, latitude, longitude, 100.0, null, 80.0);
    }
}