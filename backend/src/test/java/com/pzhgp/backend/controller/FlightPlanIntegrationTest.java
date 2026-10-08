package com.pzhgp.backend.controller;

import tools.jackson.databind.json.JsonMapper;
import com.pzhgp.backend.dto.FlightPlanCreateRequest;
import com.pzhgp.backend.dto.FlightPlanEntryRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FlightPlanEntryRepository;
import com.pzhgp.backend.repository.FlightPlanRepository;
import com.pzhgp.backend.repository.FlightResultRepository;
import com.pzhgp.backend.repository.SectionRepository;
import com.pzhgp.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Flight Plan Integration Tests")
class FlightPlanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlightPlanRepository flightPlanRepository;

    @Autowired
    private FlightPlanEntryRepository flightPlanEntryRepository;

    @Autowired
    private FlightResultRepository flightResultRepository;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JsonMapper objectMapper;

    private Section section;

    private Breeder administrator;
    private Breeder moderator;
    private Breeder breeder;

    private String adminToken;
    private String moderatorToken;
    private String breederToken;

    @BeforeEach
    void setUp() {
        section = new Section();
        section.setName("Flight Plan Test Section");
        section.setSortOrder(90);
        sectionRepository.save(section);

        administrator = createRealUser("flight-admin@test.pl", "911111111", Role.ADMINISTRATOR);
        moderator = createRealUser("flight-moderator@test.pl", "922222222", Role.MODERATOR);
        breeder = createRealUser("flight-breeder@test.pl", "933333333", Role.BREEDER);

        adminToken = jwtService.generateToken(administrator);
        moderatorToken = jwtService.generateToken(moderator);
        breederToken = jwtService.generateToken(breeder);
    }

    @Test
    @DisplayName("Unauthenticated user cannot access flight plans")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/flight-plans"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/flight-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FlightPlanCreateRequest(2026))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authenticated users can read flight plans")
    void allowsAuthenticatedUsersToReadPlans() throws Exception {
        createPlan(2026);

        mockMvc.perform(get("/api/flight-plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(breederToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].year").value(2026));

        mockMvc.perform(get("/api/flight-plans").header(HttpHeaders.AUTHORIZATION, bearer(moderatorToken))).andExpect(status().isOk());
        mockMvc.perform(get("/api/flight-plans").header(HttpHeaders.AUTHORIZATION, bearer(adminToken))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Administrator can create a flight plan in the real database")
    void createsPlan() throws Exception {
        mockMvc.perform(post("/api/flight-plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FlightPlanCreateRequest(2026))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        FlightPlan saved = flightPlanRepository.findByYear(2026).orElseThrow();
        assertEquals(2026, saved.getYear());
    }

    @Test
    @DisplayName("Moderator and Breeder cannot create flight plans")
    void rejectsPlanCreationByNonAdministrator() throws Exception {
        FlightPlanCreateRequest request = new FlightPlanCreateRequest(2026);

        mockMvc.perform(post("/api/flight-plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/flight-plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(breederToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertEquals(0, flightPlanRepository.count());
    }

    @Test
    @DisplayName("Current database role is checked even when JWT was issued for Administrator")
    void usesCurrentDatabaseRoleWhenCreatingPlan() throws Exception {
        administrator.setRole(Role.BREEDER);
        breederRepository.save(administrator);

        mockMvc.perform(post("/api/flight-plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FlightPlanCreateRequest(2026))))
                .andExpect(status().isForbidden());

        assertEquals(0, flightPlanRepository.count());
    }

    @Test
    @DisplayName("Flight plan year validation rejects values outside allowed range")
    void validatesPlanYear() throws Exception {
        mockMvc.perform(post("/api/flight-plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FlightPlanCreateRequest(1999))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/flight-plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FlightPlanCreateRequest(2101))))
                .andExpect(status().isBadRequest());

        assertEquals(0, flightPlanRepository.count());
    }

    @Test
    @DisplayName("Creating a second plan for the same year returns 400 Bad Request")
    void rejectsDuplicatePlanYear() throws Exception {
        createPlan(2026);

        mockMvc.perform(post("/api/flight-plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FlightPlanCreateRequest(2026))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Plan lotów dla roku 2026 już istnieje."));

        assertEquals(1, flightPlanRepository.count());
    }

    @Test
    @DisplayName("Administrator can add a flight and values are normalized before saving")
    void addsFlight() throws Exception {
        FlightPlan plan = createPlan(2026);

        FlightPlanEntryRequest request = new FlightPlanEntryRequest(
                PigeonAgeGroup.ADULT,
                LocalDate.of(2026, 4, 26),
                "  Dahme  ",
                130,
                "  A  ",
                "  Oddziałowa  "
        );

        mockMvc.perform(post("/api/flight-plans/2026/entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        var flights = flightPlanEntryRepository.findAllByFlightPlanIdOrderByScheduledDateAscIdAsc(plan.getId());

        assertEquals(1, flights.size());

        FlightPlanEntry saved = flights.getFirst();

        assertEquals("Dahme", saved.getLocation());
        assertEquals(130, saved.getDistanceKm());
        assertEquals("A", saved.getCategory());
        assertEquals("Oddziałowa", saved.getListType());
        assertEquals(PigeonAgeGroup.ADULT, saved.getPigeonAgeGroup());
    }

    @Test
    @DisplayName("Moderator cannot add flights to a plan")
    void rejectsFlightCreationByModerator() throws Exception {
        FlightPlan plan = createPlan(2026);

        FlightPlanEntryRequest request = request(
                PigeonAgeGroup.ADULT,
                LocalDate.of(2026, 4, 26),
                "Dahme"
        );

        mockMvc.perform(post("/api/flight-plans/2026/entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertTrue(flightPlanEntryRepository
                .findAllByFlightPlanIdOrderByScheduledDateAscIdAsc(plan.getId())
                .isEmpty());
    }

    @Test
    @DisplayName("Flight date outside plan year returns 400 Bad Request")
    void rejectsFlightFromDifferentYear() throws Exception {
        createPlan(2026);

        FlightPlanEntryRequest request = request(
                PigeonAgeGroup.ADULT,
                LocalDate.of(2025, 12, 31),
                "Dahme"
        );

        mockMvc.perform(post("/api/flight-plans/2026/entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Data lotu musi należeć do roku planu: 2026."));
    }

    @Test
    @DisplayName("Bean Validation rejects invalid flight data")
    void validatesFlightRequest() throws Exception {
        createPlan(2026);

        FlightPlanEntryRequest invalidRequest = new FlightPlanEntryRequest(
                PigeonAgeGroup.ADULT,
                LocalDate.of(2026, 4, 26),
                "",
                0,
                null,
                ""
        );

        mockMvc.perform(post("/api/flight-plans/2026/entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        assertEquals(0, flightPlanEntryRepository.count());
    }

    @Test
    @DisplayName("Flight plan details are sorted by scheduled date and separated by age group")
    void returnsFlightsSortedByDate() throws Exception {
        FlightPlan plan = createPlan(2026);

        createFlight(plan, PigeonAgeGroup.ADULT, LocalDate.of(2026, 5, 10), "Dessau");
        createFlight(plan, PigeonAgeGroup.YOUNG, LocalDate.of(2026, 8, 2), "Young Jessen");
        createFlight(plan, PigeonAgeGroup.ADULT, LocalDate.of(2026, 4, 26), "Dahme");

        mockMvc.perform(get("/api/flight-plans/2026").header(HttpHeaders.AUTHORIZATION, bearer(breederToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.adultFlights.length()").value(2))
                .andExpect(jsonPath("$.adultFlights[0].location").value("Dahme"))
                .andExpect(jsonPath("$.adultFlights[0].scheduledDate").value("2026-04-26"))
                .andExpect(jsonPath("$.adultFlights[1].location").value("Dessau"))
                .andExpect(jsonPath("$.adultFlights[1].scheduledDate").value("2026-05-10"))
                .andExpect(jsonPath("$.youngFlights.length()").value(1))
                .andExpect(jsonPath("$.youngFlights[0].location").value("Young Jessen"))
                .andExpect(jsonPath("$.adultFlights[0].sortOrder").doesNotExist());
    }

    @Test
    @DisplayName("Administrator can update an existing flight")
    void updatesFlight() throws Exception {
        FlightPlan plan = createPlan(2026);
        FlightPlanEntry entry = createFlight(
                plan,
                PigeonAgeGroup.ADULT,
                LocalDate.of(2026, 4, 26),
                "Dahme"
        );

        FlightPlanEntryRequest request = new FlightPlanEntryRequest(
                PigeonAgeGroup.YOUNG,
                LocalDate.of(2026, 8, 16),
                "  Helmstedt  ",
                310,
                null,
                "  Oddział/Dubel  "
        );

        mockMvc.perform(put("/api/flight-plans/entries/" + entry.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        FlightPlanEntry updated = flightPlanEntryRepository.findById(entry.getId()).orElseThrow();

        assertEquals(PigeonAgeGroup.YOUNG, updated.getPigeonAgeGroup());
        assertEquals(LocalDate.of(2026, 8, 16), updated.getScheduledDate());
        assertEquals("Helmstedt", updated.getLocation());
        assertEquals(310, updated.getDistanceKm());
        assertNull(updated.getCategory());
        assertEquals("Oddział/Dubel", updated.getListType());
    }

    @Test
    @DisplayName("Administrator can delete a flight without results")
    void deletesFlightWithoutResults() throws Exception {
        FlightPlan plan = createPlan(2026);
        FlightPlanEntry entry = createFlight(
                plan,
                PigeonAgeGroup.ADULT,
                LocalDate.of(2026, 4, 26),
                "Dahme"
        );

        mockMvc.perform(delete("/api/flight-plans/entries/" + entry.getId()).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        assertFalse(flightPlanEntryRepository.existsById(entry.getId()));
    }

    @Test
    @DisplayName("Flight containing results cannot be deleted and returns 409 Conflict")
    void rejectsDeletingFlightWithResults() throws Exception {
        FlightPlan plan = createPlan(2026);
        FlightPlanEntry entry = createFlight(
                plan,
                PigeonAgeGroup.ADULT,
                LocalDate.of(2026, 4, 26),
                "Dahme"
        );

        FlightResult result = new FlightResult();
        result.setFlightPlanEntry(entry);
        result.setScope(FlightResultScope.BRANCH);
        result.setOriginalFileName("wyniki.txt");
        result.setContent("Treść");
        result.setUploadedBy(administrator);
        flightResultRepository.save(result);

        mockMvc.perform(delete("/api/flight-plans/entries/" + entry.getId()).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Nie można usunąć lotu z planu, ponieważ posiada przypisane wyniki. Najpierw usuń wyniki tego lotu."));

        assertTrue(flightPlanEntryRepository.existsById(entry.getId()));
    }

    @Test
    @DisplayName("Empty flight plan can be deleted")
    void deletesEmptyPlan() throws Exception {
        createPlan(2026);

        mockMvc.perform(delete("/api/flight-plans/2026").header(HttpHeaders.AUTHORIZATION, bearer(adminToken))).andExpect(status().isNoContent());
        assertTrue(flightPlanRepository.findByYear(2026).isEmpty());
    }

    @Test
    @DisplayName("Flight plan containing flights cannot be deleted and returns 409 Conflict")
    void rejectsDeletingNonEmptyPlan() throws Exception {
        FlightPlan plan = createPlan(2026);
        createFlight(plan, PigeonAgeGroup.ADULT, LocalDate.of(2026, 4, 26), "Dahme");

        mockMvc.perform(delete("/api/flight-plans/2026").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Nie można usunąć planu lotów, który zawiera loty. Najpierw usuń wszystkie pozycje planu."));

        assertTrue(flightPlanRepository.findByYear(2026).isPresent());
    }

    @Test
    @DisplayName("Missing flight plan returns 404 Not Found")
    void returnsNotFoundForMissingPlan() throws Exception {
        mockMvc.perform(get("/api/flight-plans/2099").header(HttpHeaders.AUTHORIZATION, bearer(breederToken)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Nie znaleziono planu lotów dla roku 2099."));
    }

    private Breeder createRealUser(String email, String phoneNumber, Role role) {
        Breeder user = new Breeder();
        user.setEmail(email);
        user.setName("Test");
        user.setSurname("User");
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash("hashed");
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setSection(section);
        return breederRepository.save(user);
    }

    private FlightPlan createPlan(int year) {
        FlightPlan plan = new FlightPlan();
        plan.setYear(year);
        return flightPlanRepository.save(plan);
    }

    private FlightPlanEntry createFlight(
            FlightPlan plan,
            PigeonAgeGroup ageGroup,
            LocalDate date,
            String location
    ) {
        FlightPlanEntry entry = new FlightPlanEntry();
        entry.setFlightPlan(plan);
        entry.setPigeonAgeGroup(ageGroup);
        entry.setScheduledDate(date);
        entry.setLocation(location);
        entry.setDistanceKm(150);
        entry.setCategory("A");
        entry.setListType("Oddziałowa");
        return flightPlanEntryRepository.save(entry);
    }

    private FlightPlanEntryRequest request(PigeonAgeGroup ageGroup, LocalDate date, String location) {
        return new FlightPlanEntryRequest(ageGroup, date, location, 150, "A", "Oddziałowa");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}