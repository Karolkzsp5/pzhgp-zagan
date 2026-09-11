package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.FlightDetailsDto;
import com.pzhgp.backend.dto.FlightUploadRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FlightTrackPointRepository;
import com.pzhgp.backend.repository.PigeonFlightRepository;
import com.pzhgp.backend.service.gpx.FlightAnalyzer;
import com.pzhgp.backend.service.gpx.GpxParser;
import com.pzhgp.backend.service.gpx.GpxParsingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    @DisplayName("Wgrany plik GPX zapisuje lot wraz z kompletem punktów trasy")
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
        assertEquals(184.0, saved.getStraightLineDistanceMeters() / 1000, 1.0);
        assertEquals(1378.0, saved.getRacingVelocityMetersPerMinute(), 15.0);
    }

    @Test
    @DisplayName("Numer obrączki i nazwa lotu są odczytywane z pliku, gdy hodowca ich nie poda")
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
    @DisplayName("Dane podane przez hodowcę mają pierwszeństwo przed metadanymi pliku")
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
    @DisplayName("Plik o rozszerzeniu innym niż .gpx jest odrzucany")
    void rejectsNonGpxFile() {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));

        MockMultipartFile file = new MockMultipartFile("file", "trasa.txt",
                "text/plain", "cokolwiek".getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> pigeonFlightService.uploadFlight(file, null, "jan@example.com"));

        assertTrue(exception.getMessage().contains(".gpx"));
        verify(flightRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pusty plik jest odrzucany")
    void rejectsEmptyFile() {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));

        MockMultipartFile file = new MockMultipartFile("file", "trasa.gpx",
                "application/gpx+xml", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> pigeonFlightService.uploadFlight(file, null, "jan@example.com"));
    }

    @Test
    @DisplayName("Plik niebędący poprawnym GPX-em kończy się błędem walidacji, nie błędem serwera")
    void rejectsGarbageContent() {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.countByOwner(owner)).thenReturn(0L);

        MockMultipartFile file = new MockMultipartFile("file", "trasa.gpx",
                "application/gpx+xml", "to nie jest XML".getBytes(StandardCharsets.UTF_8));

        assertThrows(GpxParsingException.class,
                () -> pigeonFlightService.uploadFlight(file, null, "jan@example.com"));
    }

    @Test
    @DisplayName("Po przekroczeniu limitu lotów wgrywanie jest blokowane")
    void enforcesFlightLimit() throws IOException {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.countByOwner(owner)).thenReturn(200L);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> pigeonFlightService.uploadFlight(realGpxFile(), null, "jan@example.com"));

        assertTrue(exception.getMessage().contains("limit"));
        verify(flightRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nazwa pliku ze ścieżką jest oczyszczana przed zapisem")
    void sanitizesFileName() throws IOException {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.countByOwner(owner)).thenReturn(0L);
        when(flightRepository.save(any(PigeonFlight.class))).thenAnswer(i -> i.getArgument(0));

        try (InputStream stream = getClass().getResourceAsStream("/gpx/skyleader-minimalny.gpx")) {
            MockMultipartFile file = new MockMultipartFile("file", "../../../etc/podstepny.gpx",
                    "application/gpx+xml", stream.readAllBytes());

            pigeonFlightService.uploadFlight(file, null, "jan@example.com");
        }

        ArgumentCaptor<PigeonFlight> captor = ArgumentCaptor.forClass(PigeonFlight.class);
        verify(flightRepository).save(captor.capture());

        assertEquals("podstepny.gpx", captor.getValue().getOriginalFileName());
    }

    @Test
    @DisplayName("Hodowca nie widzi lotu innego hodowcy")
    void deniesAccessToForeignFlight() {
        PigeonFlight flight = flightOwnedBy(owner);
        when(breederRepository.findByEmail("adam@example.com")).thenReturn(Optional.of(otherBreeder));
        when(flightRepository.findWithOwnerById(7L)).thenReturn(Optional.of(flight));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> pigeonFlightService.getFlightDetails(7L, "adam@example.com", null));

        assertTrue(exception.getMessage().contains("uprawnień"));
    }

    @Test
    @DisplayName("Administrator może obejrzeć lot dowolnego hodowcy")
    void allowsAdministratorToViewAnyFlight() {
        PigeonFlight flight = flightOwnedBy(owner);
        when(breederRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(administrator));
        when(flightRepository.findWithOwnerById(7L)).thenReturn(Optional.of(flight));
        when(trackPointRepository.findByFlightIdOrderByPointIndexAsc(7L)).thenReturn(List.of(
                trackPoint(0, 51.0, 15.0),
                trackPoint(1, 51.1, 15.1),
                trackPoint(2, 51.2, 15.2)));

        FlightDetailsDto details = pigeonFlightService.getFlightDetails(7L, "admin@example.com", null);

        assertEquals("Jan Kowalski", details.ownerName());
        assertTrue(details.canDelete());
    }

    @Test
    @DisplayName("Hodowca nie może usunąć cudzego lotu, administrator może")
    void enforcesDeletePermissions() {
        PigeonFlight flight = flightOwnedBy(owner);
        when(flightRepository.findWithOwnerById(7L)).thenReturn(Optional.of(flight));
        when(breederRepository.findByEmail("adam@example.com")).thenReturn(Optional.of(otherBreeder));

        assertThrows(IllegalStateException.class,
                () -> pigeonFlightService.deleteFlight(7L, "adam@example.com"));
        verify(flightRepository, never()).delete(any());

        when(breederRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(administrator));
        pigeonFlightService.deleteFlight(7L, "admin@example.com");
        verify(flightRepository).delete(flight);
    }

    @Test
    @DisplayName("Żądanie nieistniejącego lotu kończy się błędem 404")
    void reportsMissingFlight() {
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.findWithOwnerById(999L)).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> pigeonFlightService.getFlightDetails(999L, "jan@example.com", null));
    }

    @Test
    @DisplayName("Tolerancja 0 zwraca pełną trasę, tolerancja dodatnia ją upraszcza")
    void appliesSimplificationTolerance() {
        PigeonFlight flight = flightOwnedBy(owner);
        when(breederRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(owner));
        when(flightRepository.findWithOwnerById(7L)).thenReturn(Optional.of(flight));

        List<FlightTrackPoint> collinear = java.util.stream.IntStream.range(0, 30)
                .mapToObj(i -> trackPoint(i, 51.0 + i * 0.001, 15.0))
                .toList();
        when(trackPointRepository.findByFlightIdOrderByPointIndexAsc(7L)).thenReturn(collinear);

        FlightDetailsDto full = pigeonFlightService.getFlightDetails(7L, "jan@example.com", 0.0);
        FlightDetailsDto simplified = pigeonFlightService.getFlightDetails(7L, "jan@example.com", 10.0);

        assertEquals(30, full.returnedPoints());
        assertEquals(2, simplified.returnedPoints());
    }

    private PigeonFlight flightOwnedBy(Breeder breeder) {
        PigeonFlight flight = new PigeonFlight();
        flight.setId(7L);
        flight.setOwner(breeder);
        flight.setName("Lot testowy");
        flight.setOriginalFileName("test.gpx");
        return flight;
    }

    private FlightTrackPoint trackPoint(int index, double latitude, double longitude) {
        return new FlightTrackPoint(index, latitude, longitude, 100.0, null, 80.0);
    }
}
