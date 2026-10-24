package com.pzhgp.backend.controller;

import tools.jackson.databind.json.JsonMapper;
import com.pzhgp.backend.dto.FlightResultUploadRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FlightPlanEntryRepository;
import com.pzhgp.backend.repository.FlightPlanRepository;
import com.pzhgp.backend.repository.FlightResultRepository;
import com.pzhgp.backend.repository.NotificationRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static com.pzhgp.backend.support.CookieAuthTestSupport.cookieAuth;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Flight Result Integration Tests")
class FlightResultIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlightResultRepository flightResultRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FlightPlanRepository flightPlanRepository;

    @Autowired
    private FlightPlanEntryRepository flightPlanEntryRepository;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JsonMapper objectMapper;

    private Section sectionOne;
    private Section sectionThree;

    private Breeder administrator;
    private Breeder moderator;
    private Breeder breeder;

    private FlightPlan plan;
    private FlightPlanEntry flight;

    private String adminToken;
    private String moderatorToken;
    private String breederToken;

    @BeforeEach
    void setUp() {
        sectionOne = new Section();
        sectionOne.setName("Flight Result Żagań");
        sectionOne.setSortOrder(1);
        sectionRepository.save(sectionOne);

        sectionThree = new Section();
        sectionThree.setName("Flight Result Chotków");
        sectionThree.setSortOrder(3);
        sectionRepository.save(sectionThree);

        administrator = createRealUser("result-admin@test.pl", "944444444", Role.ADMINISTRATOR);
        moderator = createRealUser("result-moderator@test.pl", "955555555", Role.MODERATOR);
        breeder = createRealUser("result-breeder@test.pl", "966666666", Role.BREEDER);

        adminToken = jwtService.generateToken(administrator);
        moderatorToken = jwtService.generateToken(moderator);
        breederToken = jwtService.generateToken(breeder);

        plan = new FlightPlan();
        plan.setYear(2026);
        plan = flightPlanRepository.save(plan);

        flight = new FlightPlanEntry();
        flight.setFlightPlan(plan);
        flight.setPigeonAgeGroup(PigeonAgeGroup.ADULT);
        flight.setScheduledDate(LocalDate.of(2026, 4, 26));
        flight.setLocation("Dahme");
        flight.setDistanceKm(130);
        flight.setCategory("A");
        flight.setListType("Oddziałowa");
        flight = flightPlanEntryRepository.save(flight);
    }

    @Test
    @DisplayName("Should return 401 when accessing flight results without authentication")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/flight-results/999/file")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/flight-results/999")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow an administrator to upload and save branch flight results")
    void uploadsBranchResult() throws Exception {
        String content = """
                PZHGP Oddział Żagań
                Lot: Dahme
                Oficjalne wyniki
                """;

        Long resultId = uploadResult(textFile("oddzial.txt", content), new FlightResultUploadRequest(FlightResultScope.BRANCH, null));
        FlightResult saved = flightResultRepository.findById(resultId).orElseThrow();

        assertEquals(flight.getId(), saved.getFlightPlanEntry().getId());
        assertEquals(FlightResultScope.BRANCH, saved.getScope());
        assertNull(saved.getSection());
        assertEquals("oddzial.txt", saved.getOriginalFileName());
        assertEquals(content, saved.getContent());
        assertEquals(administrator.getId(), saved.getUploadedBy().getId());
        assertNotNull(saved.getUploadedAt());
    }

    @Test
    @DisplayName("Should allow an administrator to upload flight results for a selected section")
    void uploadsSectionResult() throws Exception {
        Long resultId = uploadResult(textFile("sekcja3.txt", "Lista konkursowa Sekcji 3"), new FlightResultUploadRequest(FlightResultScope.SECTION, sectionThree.getId()));

        FlightResult saved = flightResultRepository.findById(resultId).orElseThrow();

        assertEquals(FlightResultScope.SECTION, saved.getScope());
        assertEquals(sectionThree.getId(), saved.getSection().getId());
        assertEquals("sekcja3.txt", saved.getOriginalFileName());
    }

    @Test
    @DisplayName("Should notify active users except the uploader when branch results are uploaded")
    void createsNotificationsForBranchResults() throws Exception {
        uploadResult(textFile("oddzial.txt", "Wyniki oddziałowe"),
                new FlightResultUploadRequest(FlightResultScope.BRANCH, null));

        var notifications = notificationRepository.findAll();
        assertEquals(2, notifications.size());
        assertTrue(notifications.stream().allMatch(n -> n.getType() == NotificationType.NEW_FLIGHT_RESULT));
        assertTrue(notifications.stream().allMatch(n -> n.getMessage().equals("Dodano wyniki oddziałowe lotu z Dahme (26.04.2026).")));
        assertTrue(notifications.stream().allMatch(n -> n.getLink().equals("/flight-plans/2026")));
        assertTrue(notifications.stream().noneMatch(n -> n.getRecipient().getId().equals(administrator.getId())));
        assertTrue(notifications.stream().anyMatch(n -> n.getRecipient().getId().equals(moderator.getId())));
        assertTrue(notifications.stream().anyMatch(n -> n.getRecipient().getId().equals(breeder.getId())));
    }

    @Test
    @DisplayName("Should notify only active breeders in the selected section when section results are uploaded")
    void createsNotificationsForSectionResults() throws Exception {
        Breeder chotkowBreeder = createRealUser("result-chotkow@test.pl", "977777777", Role.BREEDER);
        chotkowBreeder.setSection(sectionThree);
        breederRepository.save(chotkowBreeder);

        uploadResult(textFile("sekcja3.txt", "Wyniki sekcyjne"),
                new FlightResultUploadRequest(FlightResultScope.SECTION, sectionThree.getId()));

        var notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        Notification notification = notifications.getFirst();
        assertEquals(chotkowBreeder.getId(), notification.getRecipient().getId());
        assertEquals(NotificationType.NEW_FLIGHT_RESULT, notification.getType());
        assertEquals("Dodano wyniki sekcji " + sectionThree.getName() + " dla lotu z Dahme (26.04.2026).", notification.getMessage());
        assertEquals("/flight-plans/2026", notification.getLink());
    }

    @Test
    @DisplayName("Should return 403 when a moderator or breeder uploads flight results")
    void rejectsUploadByNonAdministrator() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .with(cookieAuth(moderatorToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .with(cookieAuth(breederToken)))
                .andExpect(status().isForbidden());

        assertEquals(0, flightResultRepository.count());
    }

    @Test
    @DisplayName("Should reject a flight result upload after the administrator's database role is downgraded")
    void usesCurrentDatabaseRoleWhenUploadingResult() throws Exception {
        administrator.setRole(Role.BREEDER);
        breederRepository.save(administrator);

        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isForbidden());

        assertEquals(0, flightResultRepository.count());
    }

    @Test
    @DisplayName("Should return 409 when uploading a second branch result for the same flight")
    void rejectsDuplicateBranchResult() throws Exception {
        uploadResult(textFile("oddzial.txt", "Pierwsza lista"), new FlightResultUploadRequest(FlightResultScope.BRANCH, null));

        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("oddzial2.txt", "Druga lista"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Wyniki oddziałowe dla tego lotu zostały już wgrane."));

        assertEquals(1, flightResultRepository.count());
    }

    @Test
    @DisplayName("Should return 409 when uploading duplicate section results for the same flight")
    void rejectsDuplicateSectionResult() throws Exception {
        uploadResult(textFile("sekcja3.txt", "Pierwsza lista"), new FlightResultUploadRequest(FlightResultScope.SECTION, sectionThree.getId()));

        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("sekcja3-v2.txt", "Druga lista"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.SECTION, sectionThree.getId())))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Wyniki tej sekcji dla wybranego lotu zostały już wgrane."));

        assertEquals(1, flightResultRepository.count());
    }

    @Test
    @DisplayName("Should reject a section ID when uploading branch results")
    void rejectsSectionIdForBranchResult() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, sectionOne.getId())))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Wyniki oddziałowe nie mogą być przypisane do konkretnej sekcji."));
    }

    @Test
    @DisplayName("Should reject section results without a section ID")
    void rejectsSectionResultWithoutSectionId() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.SECTION, null)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Dla wyników sekcyjnych należy wybrać sekcję."));
    }

    @Test
    @DisplayName("Should return 400 when the section ID is not positive")
    void rejectsNonPositiveSectionId() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.SECTION, -1L)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("ID sekcji musi być liczbą dodatnią."));
    }

    @Test
    @DisplayName("Should return 404 when uploading results for a non-existent section")
    void rejectsMissingSection() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.SECTION, 999999L)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when uploading results for a non-existent flight")
    void rejectsMissingFlight() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/999999")
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when uploading a result file without the TXT extension")
    void rejectsNonTxtFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "wyniki.pdf", "application/pdf", "Treść".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(file)
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Dozwolone są wyłącznie pliki z rozszerzeniem .txt."));
    }

    @Test
    @DisplayName("Should return 400 when the result file contains only whitespace")
    void rejectsBlankTextFile() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "   \n\t   "))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Przesłany plik wyników nie zawiera żadnej treści."));
    }

    @Test
    @DisplayName("Should return 400 when a TXT result file contains a NUL character")
    void rejectsBinaryLikeTextFile() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "ABC\0DEF"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Przesłany plik nie jest prawidłowym plikiem tekstowym."));
    }

    @Test
    @DisplayName("Should return 400 when the result file exceeds 5 MB")
    void rejectsOversizedFile() throws Exception {
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "wyniki.txt", "text/plain", oversized);

        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(file)
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Plik jest za duży. Maksymalny rozmiar pliku z wynikami to 5 MB."));
    }

    @Test
    @DisplayName("Should return 400 when the multipart request omits the result file")
    void rejectsMissingFilePart() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Żądanie jest niekompletne — brakuje części \"file\"."));
    }

    @Test
    @DisplayName("Should return 400 when the multipart request omits result metadata")
    void rejectsMissingMetadataPart() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Żądanie jest niekompletne — brakuje części \"metadata\"."));
    }

    @Test
    @DisplayName("Should return the stored result file as inline UTF-8 plain text")
    void returnsResultFile() throws Exception {
        String originalContent = "PZHGP Żagań\nZażółć gęślą jaźń\n12345";
        Long resultId = uploadResult(textFile("wyniki-dahme.txt", originalContent), new FlightResultUploadRequest(FlightResultScope.BRANCH, null));

        mockMvc.perform(get("/api/flight-results/" + resultId + "/file")
                        .with(cookieAuth(breederToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(new MediaType("text", "plain", StandardCharsets.UTF_8)))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("wyniki-dahme.txt")))
                .andExpect(content().string(originalContent));
    }

    @Test
    @DisplayName("Should list branch results before section results in configured order")
    void returnsResultsInCorrectOrder() throws Exception {
        uploadResult(textFile("sekcja3.txt", "Sekcja 3"), new FlightResultUploadRequest(FlightResultScope.SECTION, sectionThree.getId()));
        uploadResult(textFile("oddzial.txt", "Oddział"), new FlightResultUploadRequest(FlightResultScope.BRANCH, null));
        uploadResult(textFile("sekcja1.txt", "Sekcja 1"), new FlightResultUploadRequest(FlightResultScope.SECTION, sectionOne.getId()));

        mockMvc.perform(get("/api/flight-plans/2026")
                        .with(cookieAuth(breederToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adultFlights[0].results.length()").value(3))
                .andExpect(jsonPath("$.adultFlights[0].results[0].scope").value("BRANCH"))
                .andExpect(jsonPath("$.adultFlights[0].results[0].originalFileName").value("oddzial.txt"))
                .andExpect(jsonPath("$.adultFlights[0].results[1].scope").value("SECTION"))
                .andExpect(jsonPath("$.adultFlights[0].results[1].sectionSortOrder").value(1))
                .andExpect(jsonPath("$.adultFlights[0].results[2].sectionSortOrder").value(3));
    }

    @Test
    @DisplayName("Should allow an administrator to delete uploaded flight results")
    void deletesResult() throws Exception {
        Long resultId = uploadResult(textFile("wyniki.txt", "Treść"), new FlightResultUploadRequest(FlightResultScope.BRANCH, null));

        mockMvc.perform(delete("/api/flight-results/" + resultId).with(cookieAuth(adminToken))).andExpect(status().isNoContent());
        assertTrue(flightResultRepository.findById(resultId).isEmpty());
    }

    @Test
    @DisplayName("Should return 403 when a moderator deletes flight results")
    void rejectsDeletingResultByModerator() throws Exception {
        Long resultId = uploadResult(textFile("wyniki.txt", "Treść"), new FlightResultUploadRequest(FlightResultScope.BRANCH, null));

        mockMvc.perform(delete("/api/flight-results/" + resultId).with(cookieAuth(moderatorToken))).andExpect(status().isForbidden());
        assertTrue(flightResultRepository.findById(resultId).isPresent());
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
        user.setSection(sectionOne);
        return breederRepository.save(user);
    }

    private MockMultipartFile textFile(String fileName, String content) {
        return new MockMultipartFile("file", fileName, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile metadataPart(FlightResultUploadRequest request) throws IOException {
        return new MockMultipartFile("metadata", "metadata", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));
    }

    private Long uploadResult(MockMultipartFile file, FlightResultUploadRequest metadata) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(file)
                        .file(metadataPart(metadata))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}