package com.pzhgp.backend.controller;

import tools.jackson.databind.json.JsonMapper;
import com.pzhgp.backend.dto.FlightUploadRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FlightTrackPointRepository;
import com.pzhgp.backend.repository.PigeonFlightRepository;
import com.pzhgp.backend.repository.SectionRepository;
import com.pzhgp.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PigeonFlightIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private PigeonFlightRepository flightRepository;

    @Autowired
    private FlightTrackPointRepository trackPointRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JsonMapper objectMapper;

    private String ownerToken;
    private String otherToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        Section section = new Section();
        section.setName("Sekcja Lotowa");
        section.setSortOrder(1);
        sectionRepository.save(section);

        Breeder owner = breeder(section, "wlasciciel@test.pl", "Jan", "Kowalski", "111222333", Role.BREEDER);
        Breeder other = breeder(section, "inny@test.pl", "Adam", "Nowak", "444555666", Role.BREEDER);
        Breeder admin = breeder(section, "admin@test.pl", "Piotr", "Adminowski", "777888999", Role.ADMINISTRATOR);

        ownerToken = jwtService.generateToken(owner);
        otherToken = jwtService.generateToken(other);
        adminToken = jwtService.generateToken(admin);
    }

    private Breeder breeder(Section section, String email, String name, String surname,
                            String phone, Role role) {
        Breeder breeder = new Breeder();
        breeder.setEmail(email);
        breeder.setName(name);
        breeder.setSurname(surname);
        breeder.setPhoneNumber(phone);
        breeder.setPasswordHash("hashed");
        breeder.setRole(role);
        breeder.setStatus(AccountStatus.ACTIVE);
        breeder.setSection(section);
        return breederRepository.save(breeder);
    }

    private MockMultipartFile gpxFile(String resource, String fileName) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/gpx/" + resource)) {
            assertNotNull(stream, "Brak pliku testowego: " + resource);
            return new MockMultipartFile("file", fileName, "application/gpx+xml", stream.readAllBytes());
        }
    }

    private MockMultipartFile metadataPart(FlightUploadRequest request) throws IOException {
        return new MockMultipartFile("metadata", "metadata", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
    }

    private Long uploadRealFlight() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/flights")
                        .file(gpxFile("skyleader-lot-konkursowy.gpx", "Lot_konkursowy.gpx"))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("Unauthenticated user cannot upload a file or view flights")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/flights")
                        .file(gpxFile("skyleader-minimalny.gpx", "trasa.gpx")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/flights"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Breeder uploads GPX file and receives saved flight identifier")
    void uploadsFlight() throws Exception {
        Long flightId = uploadRealFlight();

        PigeonFlight saved = flightRepository.findById(flightId).orElseThrow();
        assertEquals("8414", saved.getRingNumber());
        assertEquals(1230, saved.getTotalPoints());
        assertEquals(1230, trackPointRepository.findByFlightIdOrderByPointIndexAsc(flightId).size());
    }

    @Test
    @DisplayName("Flight details contain statistics calculated from the entire recorded track")
    void returnsFlightStatistics() throws Exception {
        Long flightId = uploadRealFlight();

        mockMvc.perform(get("/api/flights/" + flightId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ringNumber").value("8414"))
                .andExpect(jsonPath("$.ownerName").value("Jan Kowalski"))
                .andExpect(jsonPath("$.statistics.totalPoints").value(1230))
                .andExpect(jsonPath("$.statistics.timestampsAvailable").value(true))
                .andExpect(jsonPath("$.statistics.straightLineDistanceKm").value(
                        org.hamcrest.Matchers.closeTo(184.4, 1.5)))
                .andExpect(jsonPath("$.statistics.trackDistanceKm").value(
                        org.hamcrest.Matchers.closeTo(195.3, 1.5)))
                .andExpect(jsonPath("$.statistics.averageSpeedMetersPerMinute").value(
                        org.hamcrest.Matchers.closeTo(638.0, 20.0)))
                .andExpect(jsonPath("$.statistics.maxSpeedMetersPerMinute").value(
                        org.hamcrest.Matchers.closeTo(1754.0, 60.0)));
    }

    @Test
    @DisplayName("Track is simplified by default and tolerance=0 returns all points")
    void appliesTrackSimplification() throws Exception {
        Long flightId = uploadRealFlight();

        MvcResult simplified = mockMvc.perform(get("/api/flights/" + flightId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult full = mockMvc.perform(get("/api/flights/" + flightId)
                        .param("tolerance", "0")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedPoints").value(1230))
                .andReturn();

        int simplifiedPoints = objectMapper.readTree(simplified.getResponse().getContentAsString())
                .get("returnedPoints").asInt();
        int fullPoints = objectMapper.readTree(full.getResponse().getContentAsString())
                .get("returnedPoints").asInt();

        assertTrue(simplifiedPoints < fullPoints,
                "Uproszczona trasa powinna zawierać mniej punktów niż pełna");
    }

    @Test
    @DisplayName("User-provided metadata is saved with the flight")
    void savesUserProvidedMetadata() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/flights")
                        .file(gpxFile("skyleader-minimalny.gpx", "trasa.gpx"))
                        .file(metadataPart(new FlightUploadRequest("Lot z Dessau", "PL-0208-24-99", "Dessau")))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andReturn();

        Long flightId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/flights/" + flightId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lot z Dessau"))
                .andExpect(jsonPath("$.ringNumber").value("PL-0208-24-99"))
                .andExpect(jsonPath("$.releaseSite").value("Dessau"));
    }

    @Test
    @DisplayName("Invalid ring number in metadata is rejected by validation")
    void rejectsInvalidMetadata() throws Exception {
        mockMvc.perform(multipart("/api/flights")
                        .file(gpxFile("skyleader-minimalny.gpx", "trasa.gpx"))
                        .file(metadataPart(new FlightUploadRequest("Lot", "obrączka<script>", null)))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("File with extension other than .gpx is rejected with 400 Bad Request")
    void rejectsNonGpxUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "zlosliwy.exe",
                "application/octet-stream", "MZ".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/flights")
                        .file(file)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Malformed GPX file returns 400 Bad Request instead of server error")
    void rejectsMalformedGpx() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "uszkodzony.gpx",
                "application/gpx+xml", "<gpx><trk><trkseg><trkpt lat=".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/flights")
                        .file(file)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Flight list contains only flights belonging to authenticated breeder")
    void listsOnlyOwnFlights() throws Exception {
        uploadRealFlight();

        mockMvc.perform(get("/api/flights").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].ringNumber").value("8414"));

        mockMvc.perform(get("/api/flights").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Another breeder cannot access someone else's flight, but administrator can")
    void enforcesFlightVisibility() throws Exception {
        Long flightId = uploadRealFlight();

        mockMvc.perform(get("/api/flights/" + flightId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/flights/" + flightId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canDelete").value(true));
    }

    @Test
    @DisplayName("Owner deletes flight together with track points")
    void deletesFlightWithTrackPoints() throws Exception {
        Long flightId = uploadRealFlight();

        mockMvc.perform(delete("/api/flights/" + flightId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/flights/" + flightId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        assertTrue(flightRepository.findById(flightId).isEmpty());
        assertTrue(trackPointRepository.findByFlightIdOrderByPointIndexAsc(flightId).isEmpty());
    }

    @Test
    @DisplayName("Non-existent flight returns 404 Not Found")
    void returnsNotFoundForMissingFlight() throws Exception {
        mockMvc.perform(get("/api/flights/999999").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }
}
