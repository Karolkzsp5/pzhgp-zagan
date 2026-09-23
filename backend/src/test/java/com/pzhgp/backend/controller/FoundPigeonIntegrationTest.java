package com.pzhgp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pzhgp.backend.dto.FoundPigeonNoteRequest;
import com.pzhgp.backend.dto.FoundPigeonRequest;
import com.pzhgp.backend.dto.FoundPigeonStatusRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FoundPigeonRepository;
import com.pzhgp.backend.repository.NotificationRepository;
import com.pzhgp.backend.repository.SectionRepository;
import com.pzhgp.backend.service.JwtService;
import com.pzhgp.backend.service.SubmissionRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class FoundPigeonIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private FoundPigeonRepository foundPigeonRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SubmissionRateLimiter rateLimiter;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Breeder administrator;
    private String adminToken;
    private String breederToken;
    private String moderatorToken;

    @BeforeEach
    void setUp() {
        // The limiter is a singleton shared between tests, so each test starts from a clean slate.
        rateLimiter.reset();

        Section section = new Section();
        section.setName("Sekcja Testowa");
        section.setSortOrder(1);
        sectionRepository.save(section);

        administrator = breeder(section, "admin@test.pl", "111111111", Role.ADMINISTRATOR);
        Breeder standardBreeder = breeder(section, "hodowca@test.pl", "222222222", Role.BREEDER);
        Breeder moderator = breeder(section, "moderator@test.pl", "333333333", Role.MODERATOR);

        adminToken = jwtService.generateToken(administrator);
        breederToken = jwtService.generateToken(standardBreeder);
        moderatorToken = jwtService.generateToken(moderator);
    }

    private Breeder breeder(Section section, String email, String phone, Role role) {
        Breeder breeder = new Breeder();
        breeder.setEmail(email);
        breeder.setName("Test");
        breeder.setSurname(role.name());
        breeder.setPhoneNumber(phone);
        breeder.setPasswordHash("hashed");
        breeder.setRole(role);
        breeder.setStatus(AccountStatus.ACTIVE);
        breeder.setSection(section);
        return breederRepository.save(breeder);
    }

    private FoundPigeonRequest report(String ringNumber, String phone, String email, ReportLanguage language) {
        return new FoundPigeonRequest(ringNumber, phone, email, "Cottbus", "Niemcy",
                "Gołąb siedzi na parapecie.", language);
    }

    private void submitExpectingCreated(FoundPigeonRequest request) throws Exception {
        mockMvc.perform(post("/api/found-pigeons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private void submitExpectingBadRequest(FoundPigeonRequest request) throws Exception {
        mockMvc.perform(post("/api/found-pigeons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private Long storeReport(FoundPigeonStatus status) {
        FoundPigeonReport stored = new FoundPigeonReport();
        stored.setRingNumber("PL-0208-24-1234");
        stored.setRingNumberNormalized("PL0208241234");
        stored.setContactPhone("+48 601 234 567");
        stored.setContactEmail("finder@example.com");
        stored.setPreferredLanguage(ReportLanguage.PL);
        stored.setStatus(status);
        return foundPigeonRepository.save(stored).getId();
    }

    // --- Public submission -------------------------------------------------

    @Test
    @DisplayName("Anyone can submit a report without a JWT token")
    void submitsReportWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/found-pigeons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                report("PL-0208-24-1234", "+49 30 12345678", null, ReportLanguage.DE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        assertEquals(1, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("A submitted report receives the PENDING status automatically")
    void newReportIsPending() throws Exception {
        submitExpectingCreated(report("PL-0208-24-1234", "601234567", null, ReportLanguage.PL));

        assertEquals(FoundPigeonStatus.PENDING, foundPigeonRepository.findAll().getFirst().getStatus());
    }

    @Test
    @DisplayName("The ring number is required")
    void requiresRingNumber() throws Exception {
        submitExpectingBadRequest(report(null, "601234567", null, ReportLanguage.PL));
        submitExpectingBadRequest(report("   ", "601234567", null, ReportLanguage.PL));

        assertEquals(0, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("A report without a phone number and without an e-mail address is rejected")
    void requiresAtLeastOneContactMethod() throws Exception {
        submitExpectingBadRequest(report("PL-0208-24-1234", null, null, ReportLanguage.PL));
        submitExpectingBadRequest(report("PL-0208-24-1234", "  ", "  ", ReportLanguage.PL));

        assertEquals(0, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("A report with only a phone number is accepted")
    void acceptsPhoneOnly() throws Exception {
        submitExpectingCreated(report("PL-0208-24-1234", "601234567", null, ReportLanguage.PL));

        assertEquals(1, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("A report with only an e-mail address is accepted")
    void acceptsEmailOnly() throws Exception {
        submitExpectingCreated(report("PL-0208-24-1234", null, "finder@example.com", ReportLanguage.EN));

        assertEquals(1, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("A report with both contact methods is accepted")
    void acceptsBothContactMethods() throws Exception {
        submitExpectingCreated(
                report("PL-0208-24-1234", "+48 601 234 567", "finder@example.com", ReportLanguage.PL));

        FoundPigeonReport stored = foundPigeonRepository.findAll().getFirst();
        assertEquals("+48 601 234 567", stored.getContactPhone());
        assertEquals("finder@example.com", stored.getContactEmail());
    }

    @Test
    @DisplayName("An invalid e-mail address is rejected")
    void rejectsInvalidEmail() throws Exception {
        submitExpectingBadRequest(report("PL-0208-24-1234", null, "not-an-email", ReportLanguage.PL));
        submitExpectingBadRequest(report("PL-0208-24-1234", null, "finder@", ReportLanguage.PL));

        assertEquals(0, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("Foreign phone numbers are accepted")
    void acceptsForeignPhoneNumbers() throws Exception {
        List<String> validNumbers = List.of(
                "+49 30 12345678",      // Niemcy, z prefiksem międzynarodowym
                "+49 (0) 30 1234567",   // Niemcy, z nawiasami
                "030 12345678",         // Niemcy, zapis krajowy
                "+48 601 234 567",      // Polska
                "601-234-567",          // Polska, z myślnikami
                "+31 20 123 4567"       // Holandia
        );

        for (String number : validNumbers) {
            rateLimiter.reset();
            submitExpectingCreated(report("PL-0208-24-1234", number, null, ReportLanguage.PL));
        }

        assertEquals(validNumbers.size(), foundPigeonRepository.count());
    }

    @Test
    @DisplayName("Clearly malformed phone numbers are rejected")
    void rejectsMalformedPhoneNumbers() throws Exception {
        submitExpectingBadRequest(report("PL-0208-24-1234", "12345", null, ReportLanguage.PL));
        submitExpectingBadRequest(report("PL-0208-24-1234", "telefon", null, ReportLanguage.PL));
        submitExpectingBadRequest(
                report("PL-0208-24-1234", "+48 601 234 567 890 123 456", null, ReportLanguage.PL));

        assertEquals(0, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("Field length limits are enforced")
    void enforcesMaximumLengths() throws Exception {
        submitExpectingBadRequest(new FoundPigeonRequest("P".repeat(65), "601234567", null,
                null, null, null, ReportLanguage.PL));

        submitExpectingBadRequest(new FoundPigeonRequest("PL-0208-24-1234", "601234567", null,
                null, null, "x".repeat(1001), ReportLanguage.PL));

        submitExpectingBadRequest(new FoundPigeonRequest("PL-0208-24-1234", "601234567", null,
                "x".repeat(151), null, null, ReportLanguage.PL));

        assertEquals(0, foundPigeonRepository.count());

        // A description exactly at the limit is still accepted.
        submitExpectingCreated(new FoundPigeonRequest("PL-0208-24-1234", "601234567", null,
                null, null, "x".repeat(1000), ReportLanguage.PL));
    }

    @Test
    @DisplayName("Polish, English and German are stored as the preferred language")
    void storesAllSupportedLanguages() throws Exception {
        for (ReportLanguage language : ReportLanguage.values()) {
            rateLimiter.reset();
            submitExpectingCreated(report("PL-0208-24-" + language.name(), "601234567", null, language));
        }

        List<ReportLanguage> stored = foundPigeonRepository.findAll().stream()
                .map(FoundPigeonReport::getPreferredLanguage)
                .toList();

        assertTrue(stored.containsAll(List.of(ReportLanguage.PL, ReportLanguage.EN, ReportLanguage.DE)));
    }

    @Test
    @DisplayName("Administrators receive a notification about a new report")
    void notifiesAdministrators() throws Exception {
        submitExpectingCreated(report("PL-0208-24-1234", "601234567", null, ReportLanguage.PL));

        List<Notification> notifications = notificationRepository
                .findAllByRecipientIdOrderByCreatedAtDesc(administrator.getId());

        assertEquals(1, notifications.size());
        assertEquals(NotificationType.NEW_FOUND_PIGEON_REPORT, notifications.getFirst().getType());
        assertEquals("/found-pigeons/admin", notifications.getFirst().getLink());
        assertFalse(notifications.getFirst().getMessage().contains("601234567"),
                "The notification must not contain the finder's phone number");
    }

    @Test
    @DisplayName("The submission response returns only the identifier, never the submitted data")
    void responseDoesNotEchoSubmittedData() throws Exception {
        String response = mockMvc.perform(post("/api/found-pigeons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                report("PL-0208-24-1234", "601234567", "finder@example.com", ReportLanguage.PL))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertFalse(response.contains("601234567"));
        assertFalse(response.contains("finder@example.com"));
        assertFalse(response.contains("PL-0208-24-1234"));
    }

    @Test
    @DisplayName("Repeated submissions from one address are throttled")
    void throttlesRepeatedSubmissions() throws Exception {
        for (int i = 0; i < 5; i++) {
            submitExpectingCreated(report("PL-0208-24-000" + i, "601234567", null, ReportLanguage.PL));
        }

        mockMvc.perform(post("/api/found-pigeons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                report("PL-0208-24-9999", "601234567", null, ReportLanguage.PL))))
                .andExpect(status().isForbidden());

        assertEquals(5, foundPigeonRepository.count());
    }

    // --- Privacy and authorization ----------------------------------------

    @Test
    @DisplayName("The module exposes no public endpoint for reading reports")
    void exposesNoPublicReadEndpoint() throws Exception {
        storeReport(FoundPigeonStatus.PENDING);

        mockMvc.perform(get("/api/found-pigeons")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/found-pigeons/1")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/found-pigeons")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A standard breeder cannot reach the administration panel")
    void deniesAccessToStandardBreeder() throws Exception {
        Long reportId = storeReport(FoundPigeonStatus.PENDING);

        mockMvc.perform(get("/api/admin/found-pigeons")
                        .header("Authorization", "Bearer " + breederToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/found-pigeons/" + reportId)
                        .header("Authorization", "Bearer " + breederToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/found-pigeons/" + reportId)
                        .header("Authorization", "Bearer " + breederToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A moderator cannot reach the administration panel either")
    void deniesAccessToModerator() throws Exception {
        mockMvc.perform(get("/api/admin/found-pigeons")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
    }

    // --- Administration panel ---------------------------------------------

    @Test
    @DisplayName("The administrator sees the report together with the contact details")
    void administratorSeesContactDetails() throws Exception {
        Long reportId = storeReport(FoundPigeonStatus.PENDING);

        mockMvc.perform(get("/api/admin/found-pigeons/" + reportId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ringNumber").value("PL-0208-24-1234"))
                .andExpect(jsonPath("$.contactPhone").value("+48 601 234 567"))
                .andExpect(jsonPath("$.contactEmail").value("finder@example.com"))
                .andExpect(jsonPath("$.preferredLanguage").value("PL"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Reports can be filtered by status and searched by ring number")
    void filtersAndSearchesReports() throws Exception {
        storeReport(FoundPigeonStatus.PENDING);
        storeReport(FoundPigeonStatus.APPROVED);

        mockMvc.perform(get("/api/admin/found-pigeons")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // The search ignores letter case and separator differences.
        mockMvc.perform(get("/api/admin/found-pigeons")
                        .param("ringNumber", "pl 0208")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/admin/found-pigeons")
                        .param("ringNumber", "PL-9999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Pagination defaults to ten records and rejects a size above fifty")
    void appliesPaginationLimits() throws Exception {
        for (int i = 0; i < 12; i++) {
            storeReport(FoundPigeonStatus.PENDING);
        }

        mockMvc.perform(get("/api/admin/found-pigeons")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(12));

        // A page size above the allowed maximum is rejected instead of being silently capped.
        mockMvc.perform(get("/api/admin/found-pigeons")
                        .param("size", "500")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/found-pigeons")
                        .param("page", "-1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/found-pigeons")
                        .param("size", "50")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));

        mockMvc.perform(get("/api/admin/found-pigeons")
                        .param("page", "1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("The administrator walks a report through the allowed statuses")
    void movesReportThroughAllowedStatuses() throws Exception {
        Long reportId = storeReport(FoundPigeonStatus.PENDING);

        mockMvc.perform(patch("/api/admin/found-pigeons/" + reportId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FoundPigeonStatusRequest(FoundPigeonStatus.APPROVED)))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(patch("/api/admin/found-pigeons/" + reportId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FoundPigeonStatusRequest(FoundPigeonStatus.RESOLVED)))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("Forbidden status transitions are rejected")
    void rejectsForbiddenStatusTransitions() throws Exception {
        Long pendingId = storeReport(FoundPigeonStatus.PENDING);
        Long resolvedId = storeReport(FoundPigeonStatus.RESOLVED);

        // PENDING cannot jump straight to RESOLVED - the report has to be verified first.
        mockMvc.perform(patch("/api/admin/found-pigeons/" + pendingId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FoundPigeonStatusRequest(FoundPigeonStatus.RESOLVED)))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        // A closed report stays closed.
        mockMvc.perform(patch("/api/admin/found-pigeons/" + resolvedId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FoundPigeonStatusRequest(FoundPigeonStatus.APPROVED)))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("The administrator adds and edits the private note")
    void managesAdministratorNote() throws Exception {
        Long reportId = storeReport(FoundPigeonStatus.APPROVED);

        mockMvc.perform(patch("/api/admin/found-pigeons/" + reportId + "/note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FoundPigeonNoteRequest("Właściciel: sekcja Żagań.")))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminNote").value("Właściciel: sekcja Żagań."));

        mockMvc.perform(patch("/api/admin/found-pigeons/" + reportId + "/note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FoundPigeonNoteRequest("Kontakt przekazany hodowcy.")))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminNote").value("Kontakt przekazany hodowcy."));
    }

    @Test
    @DisplayName("The administrator deletes a report that is no longer needed")
    void deletesReport() throws Exception {
        Long reportId = storeReport(FoundPigeonStatus.RESOLVED);

        mockMvc.perform(delete("/api/admin/found-pigeons/" + reportId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertTrue(foundPigeonRepository.findById(reportId).isEmpty());
    }

    @Test
    @DisplayName("A missing report returns the not-found status")
    void returnsNotFoundForMissingReport() throws Exception {
        mockMvc.perform(get("/api/admin/found-pigeons/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("The pending counter reports how many submissions await verification")
    void countsPendingReports() throws Exception {
        storeReport(FoundPigeonStatus.PENDING);
        storeReport(FoundPigeonStatus.PENDING);
        storeReport(FoundPigeonStatus.APPROVED);

        mockMvc.perform(get("/api/admin/found-pigeons/pending-count")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }
}
