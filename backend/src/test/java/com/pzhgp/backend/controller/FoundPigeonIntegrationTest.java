package com.pzhgp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pzhgp.backend.dto.FoundPigeonNoteRequest;
import com.pzhgp.backend.dto.FoundPigeonRequest;
import com.pzhgp.backend.dto.FoundPigeonStatusRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.exception.CaptchaUnavailableException;
import com.pzhgp.backend.exception.CaptchaVerificationException;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FoundPigeonRepository;
import com.pzhgp.backend.repository.NotificationRepository;
import com.pzhgp.backend.repository.SectionRepository;
import com.pzhgp.backend.service.JwtService;
import com.pzhgp.backend.service.TurnstileService;
import com.pzhgp.backend.support.MutableClock;
import com.pzhgp.backend.support.TestClockConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

import static com.pzhgp.backend.support.CookieAuthTestSupport.cookieAuth;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@DisplayName("Found Pigeon Integration Tests")
class FoundPigeonIntegrationTest {

    private static final String CAPTCHA_TOKEN = "captcha-test-token";

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
    private MutableClock clock;

    @Autowired
    private JwtService jwtService;

    // Atrapa zamiast zapytań do Cloudflare: domyślnie przyjmuje każdy token.
    @MockitoBean
    private TurnstileService turnstileService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Breeder administrator;
    private String adminToken;
    private String breederToken;
    private String moderatorToken;

    @BeforeEach
    void setUp() {
        // Limit zgłoszeń jest wspólny dla testów; po przesunięciu zegara o dobę wcześniejsze zgłoszenia wygasają.
        clock.advance(Duration.ofDays(1));

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
        return new FoundPigeonRequest(ringNumber, phone, email, "Cottbus", "Niemcy", "Gołąb siedzi na parapecie.", language, CAPTCHA_TOKEN);
    }

    private void submitExpectingCreated(FoundPigeonRequest request) throws Exception {
        mockMvc.perform(post("/api/found-pigeons").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private void submitExpectingBadRequest(FoundPigeonRequest request) throws Exception {
        mockMvc.perform(post("/api/found-pigeons").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private Long storeReport(FoundPigeonStatus status) {
        FoundPigeonReport stored = new FoundPigeonReport();
        stored.setRingNumber("PL-0369-26-1234");
        stored.setRingNumberNormalized("PL0369261234");
        stored.setContactPhone("+48 601 234 567");
        stored.setContactEmail("finder@example.com");
        stored.setPreferredLanguage(ReportLanguage.PL);
        stored.setStatus(status);
        return foundPigeonRepository.save(stored).getId();
    }

    // --- Publiczne zgłoszenie ----------------------------------------------

    @Test
    @DisplayName("Should allow submitting a found-pigeon report without authentication")
    void submitsReportWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/found-pigeons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(report("PL-0369-26-1234", "+49 30 12345678", null, ReportLanguage.DE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        assertEquals(1, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("Should assign PENDING status to a newly submitted report")
    void newReportIsPending() throws Exception {
        submitExpectingCreated(report("PL-0369-26-1234", "601234567", null, ReportLanguage.PL));
        assertEquals(FoundPigeonStatus.PENDING, foundPigeonRepository.findAll().getFirst().getStatus());
    }

    @Test
    @DisplayName("Should return only the report ID without exposing submitted personal data")
    void responseDoesNotEchoSubmittedData() throws Exception {
        String response = mockMvc.perform(post("/api/found-pigeons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(report("PL-0369-26-1234", "601234567", "finder@example.com", ReportLanguage.PL))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertFalse(response.contains("601234567"));
        assertFalse(response.contains("finder@example.com"));
        assertFalse(response.contains("PL-0369-26-1234"));
    }

    @Test
    @DisplayName("Should verify the CAPTCHA token before storing a report")
    void verifiesCaptchaToken() throws Exception {
        submitExpectingCreated(report("PL-0369-26-1234", "601234567", null, ReportLanguage.PL));

        verify(turnstileService).verifyToken(CAPTCHA_TOKEN);
    }

    @Test
    @DisplayName("Should reject a report without a CAPTCHA token")
    void requiresCaptchaToken() throws Exception {
        submitExpectingBadRequest(new FoundPigeonRequest("PL-0369-26-1234", "601234567", null,
                null, null, null, ReportLanguage.PL, null));

        submitExpectingBadRequest(new FoundPigeonRequest("PL-0369-26-1234", "601234567", null,
                null, null, null, ReportLanguage.PL, "   "));

        verify(turnstileService, never()).verifyToken(any());
        assertEquals(0, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("Should return 400 and store nothing when the CAPTCHA token is rejected")
    void rejectsReportWhenCaptchaVerificationFails() throws Exception {
        doThrow(new CaptchaVerificationException("Weryfikacja CAPTCHA nie powiodła się. Spróbuj ponownie."))
                .when(turnstileService).verifyToken(CAPTCHA_TOKEN);

        mockMvc.perform(post("/api/found-pigeons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(report("PL-0369-26-1234", "601234567", null, ReportLanguage.PL))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Weryfikacja CAPTCHA nie powiodła się. Spróbuj ponownie."));

        assertEquals(0, foundPigeonRepository.count());
        assertEquals(0, notificationRepository.count());
    }

    @Test
    @DisplayName("Should return 503 and store nothing when the CAPTCHA service is unavailable")
    void returnsServiceUnavailableWhenCaptchaCannotBeVerified() throws Exception {
        doThrow(new CaptchaUnavailableException("Weryfikacja CAPTCHA jest chwilowo niedostępna. Spróbuj ponownie później."))
                .when(turnstileService).verifyToken(CAPTCHA_TOKEN);

        mockMvc.perform(post("/api/found-pigeons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(report("PL-0369-26-1234", "601234567", null, ReportLanguage.PL))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Weryfikacja CAPTCHA jest chwilowo niedostępna. Spróbuj ponownie później."));

        assertEquals(0, foundPigeonRepository.count());
        assertEquals(0, notificationRepository.count());
    }

    @Test
    @DisplayName("Should reject a report without a ring number")
    void requiresRingNumber() throws Exception {
        submitExpectingBadRequest(report(null, "601234567", null, ReportLanguage.PL));
        submitExpectingBadRequest(report("   ", "601234567", null, ReportLanguage.PL));

        assertEquals(0, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("Should reject a ring number that does not match the required Polish format")
    void rejectsInvalidRingNumberFormat() throws Exception {
        submitExpectingBadRequest(report("PL-0208-24-1234", "601234567", null, ReportLanguage.PL));
        submitExpectingBadRequest(report("PL-0369-2024-1234", "601234567", null, ReportLanguage.PL));
        submitExpectingBadRequest(report("PL-0369-24-123", "601234567", null, ReportLanguage.PL));
        submitExpectingBadRequest(report("PL-0369-24-12345", "601234567", null, ReportLanguage.PL));
        submitExpectingBadRequest(report("pl-0369-24-1234", "601234567", null, ReportLanguage.PL));
        assertEquals(0, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("Should reject a report without a phone number or email address")
    void requiresAtLeastOneContactMethod() throws Exception {
        submitExpectingBadRequest(report("PL-0369-26-1234", null, null, ReportLanguage.PL));
        submitExpectingBadRequest(report("PL-0369-26-1234", "  ", "  ", ReportLanguage.PL));

        assertEquals(0, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("Should accept a report containing only a phone number")
    void acceptsPhoneOnly() throws Exception {
        submitExpectingCreated(report("PL-0369-26-1234", "601234567", null, ReportLanguage.PL));
        assertEquals(1, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("Should accept a report containing only an email address")
    void acceptsEmailOnly() throws Exception {
        submitExpectingCreated(report("PL-0369-26-1234", null, "finder@example.com", ReportLanguage.EN));
        assertEquals(1, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("Should accept a report containing both phone number and email address")
    void acceptsBothContactMethods() throws Exception {
        submitExpectingCreated(report("PL-0369-26-1234", "+48 601 234 567", "finder@example.com", ReportLanguage.PL));

        FoundPigeonReport stored = foundPigeonRepository.findAll().getFirst();
        assertEquals("+48 601 234 567", stored.getContactPhone());
        assertEquals("finder@example.com", stored.getContactEmail());
    }

    @Test
    @DisplayName("Should reject a report containing an invalid email address")
    void rejectsInvalidEmail() throws Exception {
        submitExpectingBadRequest(report("PL-0369-26-1234", null, "not-an-email", ReportLanguage.PL));
        submitExpectingBadRequest(report("PL-0369-26-1234", null, "finder@", ReportLanguage.PL));

        assertEquals(0, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("Should accept supported international phone number formats")
    void acceptsForeignPhoneNumbers() throws Exception {
        List<String> validNumbers = List.of(
                "+49 30 12345678",
                "030 12345678",
                "+48 601234567",
                "601-234-567",
                "+31 20 1234567"
        );

        for (String number : validNumbers) {
            clock.advance(Duration.ofHours(1));
            submitExpectingCreated(report("PL-0369-24-1234", number, null, ReportLanguage.PL));
        }

        assertEquals(validNumbers.size(), foundPigeonRepository.count());
    }

    @Test
    @DisplayName("Should reject malformed phone numbers")
    void rejectsMalformedPhoneNumbers() throws Exception {
        submitExpectingBadRequest(report("PL-0369-26-1234", "12345", null, ReportLanguage.PL));
        submitExpectingBadRequest(report("PL-0369-26-1234", "telefon", null, ReportLanguage.PL));
        submitExpectingBadRequest(report("PL-0369-26-1234", "+48 601 234 567 890 123 456", null, ReportLanguage.PL));

        assertEquals(0, foundPigeonRepository.count());
    }

    @Test
    @DisplayName("Should enforce the maximum lengths of report fields")
    void enforcesMaximumLengths() throws Exception {
        submitExpectingBadRequest(new FoundPigeonRequest("PL-0369-24-1234", "1234567890123456", null,
                null, null, null, ReportLanguage.PL, CAPTCHA_TOKEN));

        submitExpectingBadRequest(new FoundPigeonRequest("PL-0369-24-1234", "601234567", null,
                "x".repeat(151), null, null, ReportLanguage.PL, CAPTCHA_TOKEN));

        submitExpectingBadRequest(new FoundPigeonRequest("PL-0369-24-1234", "601234567", null,
                null, "x".repeat(101), null, ReportLanguage.PL, CAPTCHA_TOKEN));

        submitExpectingBadRequest(new FoundPigeonRequest("PL-0369-24-1234", "601234567", null,
                null, null, "x".repeat(1001), ReportLanguage.PL, CAPTCHA_TOKEN));

        assertEquals(0, foundPigeonRepository.count());

        submitExpectingCreated(new FoundPigeonRequest("PL-0369-24-1234", "601234567", null,
                null, null, "x".repeat(1000), ReportLanguage.PL, CAPTCHA_TOKEN));
    }

    @Test
    @DisplayName("Should store Polish, English and German as preferred contact languages")
    void storesAllSupportedLanguages() throws Exception {
        int pigeonNumber = 1;

        for (ReportLanguage language : ReportLanguage.values()) {
            clock.advance(Duration.ofHours(1));
            String ringNumber = String.format("PL-0369-26-%04d", pigeonNumber++);
            submitExpectingCreated(report(ringNumber, "601234567", null, language));
        }

        List<ReportLanguage> stored = foundPigeonRepository.findAll().stream().map(FoundPigeonReport::getPreferredLanguage).toList();
        assertTrue(stored.containsAll(List.of(ReportLanguage.PL, ReportLanguage.EN, ReportLanguage.DE)));
    }

    @Test
    @DisplayName("Should notify administrators when a new found-pigeon report is submitted")
    void notifiesAdministrators() throws Exception {
        submitExpectingCreated(report("PL-0369-26-1234", "601234567", null, ReportLanguage.PL));

        List<Notification> notifications = notificationRepository.findByRecipientId(
                administrator.getId(),
                PageRequest.of(0, 50, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))).getContent();

        assertEquals(1, notifications.size());

        Notification notification = notifications.getFirst();

        assertEquals(NotificationType.NEW_FOUND_PIGEON_REPORT, notification.getType());
        assertEquals("/found-pigeons/admin", notification.getLink());
        assertFalse(notification.getMessage().contains("601234567"), "The notification must not contain the finder's phone number");
    }

    @Test
    @DisplayName("Should rate-limit repeated report submissions from one address")
    void throttlesRepeatedSubmissions() throws Exception {
        for (int i = 0; i < 5; i++) {
            submitExpectingCreated(report("PL-0369-26-000" + i, "601234567", null, ReportLanguage.PL));
        }

        mockMvc.perform(post("/api/found-pigeons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(report("PL-0369-26-9999", "601234567", null, ReportLanguage.PL))))
                .andExpect(status().isTooManyRequests())
                // Komunikat nie zdradza skonfigurowanego limitu.
                .andExpect(content().string("Zbyt wiele zgłoszeń. Spróbuj ponownie później."));

        assertEquals(5, foundPigeonRepository.count());

        // Zgłoszenie ponad limit zostało odrzucone, zanim serwer zapytał Cloudflare.
        verify(turnstileService, times(5)).verifyToken(CAPTCHA_TOKEN);
    }

    // --- Prywatność i uprawnienia -----------------------------------------

    @Test
    @DisplayName("Should prevent unauthenticated users from reading found-pigeon reports")
    void exposesNoPublicReadEndpoint() throws Exception {
        storeReport(FoundPigeonStatus.PENDING);

        mockMvc.perform(get("/api/found-pigeons")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/found-pigeons/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/found-pigeons")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny breeders access to found-pigeon administration")
    void deniesAccessToStandardBreeder() throws Exception {
        Long reportId = storeReport(FoundPigeonStatus.PENDING);

        mockMvc.perform(get("/api/admin/found-pigeons").with(cookieAuth(breederToken))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/found-pigeons/" + reportId).with(cookieAuth(breederToken))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/found-pigeons/" + reportId).with(cookieAuth(breederToken))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny moderators access to found-pigeon administration")
    void deniesAccessToModerator() throws Exception {
        mockMvc.perform(get("/api/admin/found-pigeons").with(cookieAuth(moderatorToken))).andExpect(status().isForbidden());
    }

    // --- Panel administratora ---------------------------------------------

    @Test
    @DisplayName("Should show contact details to administrators reviewing a report")
    void administratorSeesContactDetails() throws Exception {
        Long reportId = storeReport(FoundPigeonStatus.PENDING);

        mockMvc.perform(get("/api/admin/found-pigeons/" + reportId).with(cookieAuth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ringNumber").value("PL-0369-26-1234"))
                .andExpect(jsonPath("$.contactPhone").value("+48 601 234 567"))
                .andExpect(jsonPath("$.contactEmail").value("finder@example.com"))
                .andExpect(jsonPath("$.preferredLanguage").value("PL"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Should filter reports by status and search by ring number")
    void filtersAndSearchesReports() throws Exception {
        storeReport(FoundPigeonStatus.PENDING);
        storeReport(FoundPigeonStatus.APPROVED);

        mockMvc.perform(get("/api/admin/found-pigeons").param("status", "PENDING").with(cookieAuth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // Wyszukiwanie nie zależy od wielkości liter ani od znaków rozdzielających.
        mockMvc.perform(get("/api/admin/found-pigeons").param("ringNumber", "pl 0369").with(cookieAuth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/admin/found-pigeons").param("ringNumber", "PL-9999").with(cookieAuth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Should apply default pagination and reject page sizes above 50")
    void appliesPaginationLimits() throws Exception {
        for (int i = 0; i < 12; i++) {
            storeReport(FoundPigeonStatus.PENDING);
        }

        mockMvc.perform(get("/api/admin/found-pigeons").with(cookieAuth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(12));

        // Rozmiar strony powyżej dozwolonego maksimum jest odrzucany, a nie po cichu przycinany.
        mockMvc.perform(get("/api/admin/found-pigeons").param("size", "500").with(cookieAuth(adminToken)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/found-pigeons").param("page", "-1").with(cookieAuth(adminToken)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/found-pigeons").param("size", "50").with(cookieAuth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));

        mockMvc.perform(get("/api/admin/found-pigeons").param("page", "1").with(cookieAuth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("Should count found-pigeon reports awaiting verification")
    void countsPendingReports() throws Exception {
        storeReport(FoundPigeonStatus.PENDING);
        storeReport(FoundPigeonStatus.PENDING);
        storeReport(FoundPigeonStatus.APPROVED);

        mockMvc.perform(get("/api/admin/found-pigeons/pending-count").with(cookieAuth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    @DisplayName("Should allow an administrator to apply valid report status transitions")
    void movesReportThroughAllowedStatuses() throws Exception {
        Long reportId = storeReport(FoundPigeonStatus.PENDING);

        mockMvc.perform(patch("/api/admin/found-pigeons/" + reportId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FoundPigeonStatusRequest(FoundPigeonStatus.APPROVED)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(patch("/api/admin/found-pigeons/" + reportId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FoundPigeonStatusRequest(FoundPigeonStatus.RESOLVED)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("Should return 409 when applying an invalid report status transition")
    void rejectsForbiddenStatusTransitions() throws Exception {
        Long pendingId = storeReport(FoundPigeonStatus.PENDING);
        Long resolvedId = storeReport(FoundPigeonStatus.RESOLVED);

        mockMvc.perform(patch("/api/admin/found-pigeons/" + pendingId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FoundPigeonStatusRequest(FoundPigeonStatus.RESOLVED)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/admin/found-pigeons/" + resolvedId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FoundPigeonStatusRequest(FoundPigeonStatus.APPROVED)))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should allow an administrator to add and edit a private report note")
    void managesAdministratorNote() throws Exception {
        Long reportId = storeReport(FoundPigeonStatus.APPROVED);

        mockMvc.perform(patch("/api/admin/found-pigeons/" + reportId + "/note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FoundPigeonNoteRequest("Właściciel: sekcja Żagań.")))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminNote").value("Właściciel: sekcja Żagań."));

        mockMvc.perform(patch("/api/admin/found-pigeons/" + reportId + "/note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FoundPigeonNoteRequest("Kontakt przekazany hodowcy.")))
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminNote").value("Kontakt przekazany hodowcy."));
    }

    @Test
    @DisplayName("Should allow an administrator to delete a found-pigeon report")
    void deletesReport() throws Exception {
        Long reportId = storeReport(FoundPigeonStatus.RESOLVED);

        mockMvc.perform(delete("/api/admin/found-pigeons/" + reportId).with(cookieAuth(adminToken))).andExpect(status().isNoContent());
        assertTrue(foundPigeonRepository.findById(reportId).isEmpty());
    }

    @Test
    @DisplayName("Should return 404 when requesting a non-existent report")
    void returnsNotFoundForMissingReport() throws Exception {
        mockMvc.perform(get("/api/admin/found-pigeons/999999").with(cookieAuth(adminToken))).andExpect(status().isNotFound());
    }
}