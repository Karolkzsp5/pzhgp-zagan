package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.FoundPigeonDto;
import com.pzhgp.backend.dto.FoundPigeonRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FoundPigeonRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoundPigeonServiceTest {

    private static final String CLIENT_IP = "203.0.113.10";

    @Mock
    private FoundPigeonRepository foundPigeonRepository;

    @Mock
    private BreederRepository breederRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SubmissionRateLimiter rateLimiter;

    @InjectMocks
    private FoundPigeonService foundPigeonService;

    private Breeder administrator;

    @BeforeEach
    void setUp() {
        Section section = new Section();
        section.setId(1L);
        section.setName("Żagań");

        administrator = new Breeder();
        administrator.setId(1L);
        administrator.setName("Piotr");
        administrator.setSurname("Adminowski");
        administrator.setEmail("admin@example.com");
        administrator.setRole(Role.ADMINISTRATOR);
        administrator.setStatus(AccountStatus.ACTIVE);
        administrator.setSection(section);
    }

    private FoundPigeonRequest request(String ringNumber, String phone, String email, ReportLanguage language) {
        return new FoundPigeonRequest(ringNumber, phone, email, "Cottbus", "Niemcy",
                "Gołąb siedzi na parapecie od wczoraj.", language);
    }

    private void acceptRateLimit() {
        when(rateLimiter.tryAcquire(any())).thenReturn(true);
    }

    private void stubSave() {
        when(foundPigeonRepository.save(any(FoundPigeonReport.class))).thenAnswer(invocation -> {
            FoundPigeonReport saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(42L);
            }
            return saved;
        });
    }

    @Test
    @DisplayName("A new report is stored with the PENDING status")
    void newReportStartsAsPending() {
        acceptRateLimit();
        stubSave();
        when(breederRepository.findByRoleAndStatus(Role.ADMINISTRATOR, AccountStatus.ACTIVE))
                .thenReturn(List.of(administrator));

        Long id = foundPigeonService.createReport(
                request("PL-0208-24-1234", "+49 30 12345678", null, ReportLanguage.DE), CLIENT_IP);

        assertEquals(42L, id);

        ArgumentCaptor<FoundPigeonReport> captor = ArgumentCaptor.forClass(FoundPigeonReport.class);
        verify(foundPigeonRepository).save(captor.capture());

        assertEquals(FoundPigeonStatus.PENDING, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("The ring number is stored both as typed and in a normalized form")
    void storesOriginalAndNormalizedRingNumber() {
        acceptRateLimit();
        stubSave();
        when(breederRepository.findByRoleAndStatus(any(), any())).thenReturn(List.of());

        foundPigeonService.createReport(
                request("pl-0208-24-1234", null, "finder@example.com", ReportLanguage.PL), CLIENT_IP);

        ArgumentCaptor<FoundPigeonReport> captor = ArgumentCaptor.forClass(FoundPigeonReport.class);
        verify(foundPigeonRepository).save(captor.capture());

        assertEquals("pl-0208-24-1234", captor.getValue().getRingNumber());
        assertEquals("PL0208241234", captor.getValue().getRingNumberNormalized());
    }

    @Test
    @DisplayName("Missing language defaults to Polish")
    void defaultsToPolishLanguage() {
        acceptRateLimit();
        stubSave();
        when(breederRepository.findByRoleAndStatus(any(), any())).thenReturn(List.of());

        foundPigeonService.createReport(
                request("PL-0208-24-1234", "601234567", null, null), CLIENT_IP);

        ArgumentCaptor<FoundPigeonReport> captor = ArgumentCaptor.forClass(FoundPigeonReport.class);
        verify(foundPigeonRepository).save(captor.capture());

        assertEquals(ReportLanguage.PL, captor.getValue().getPreferredLanguage());
    }

    @Test
    @DisplayName("Active administrators are notified about a new report")
    void notifiesActiveAdministrators() {
        acceptRateLimit();
        stubSave();
        when(breederRepository.findByRoleAndStatus(Role.ADMINISTRATOR, AccountStatus.ACTIVE))
                .thenReturn(List.of(administrator));

        foundPigeonService.createReport(
                request("PL-0208-24-1234", "601234567", null, ReportLanguage.PL), CLIENT_IP);

        verify(notificationService).createBulkNotifications(
                eq(List.of(administrator)),
                any(String.class),
                eq("/found-pigeons/admin"),
                eq(NotificationType.NEW_FOUND_PIGEON_REPORT));
    }

    @Test
    @DisplayName("The notification never carries the finder's contact details")
    void notificationOmitsContactDetails() {
        acceptRateLimit();
        stubSave();
        when(breederRepository.findByRoleAndStatus(any(), any())).thenReturn(List.of(administrator));

        foundPigeonService.createReport(
                request("PL-0208-24-1234", "+48 601 234 567", "finder@example.com", ReportLanguage.PL), CLIENT_IP);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createBulkNotifications(any(), message.capture(), any(), any());

        assertFalse(message.getValue().contains("601"), "The phone number must not leak into the notification");
        assertFalse(message.getValue().contains("finder@example.com"),
                "The e-mail address must not leak into the notification");
        assertTrue(message.getValue().contains("PL-0208-24-1234"));
    }

    @Test
    @DisplayName("No notification is sent when there is no active administrator")
    void skipsNotificationWithoutAdministrators() {
        acceptRateLimit();
        stubSave();
        when(breederRepository.findByRoleAndStatus(any(), any())).thenReturn(List.of());

        foundPigeonService.createReport(
                request("PL-0208-24-1234", "601234567", null, ReportLanguage.PL), CLIENT_IP);

        verify(notificationService, never()).createBulkNotifications(any(), any(), any(), any());
    }

    @Test
    @DisplayName("A report is rejected once the submission rate limit is exceeded")
    void rejectsReportOverRateLimit() {
        when(rateLimiter.tryAcquire(CLIENT_IP)).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> foundPigeonService.createReport(
                        request("PL-0208-24-1234", "601234567", null, ReportLanguage.PL), CLIENT_IP));

        assertFalse(exception.getMessage().matches(".*\\d+.*"),
                "The message must not reveal the configured limit");
        verify(foundPigeonRepository, never()).save(any());
    }

    @Test
    @DisplayName("Allowed status transitions are accepted")
    void allowsValidStatusTransitions() {
        FoundPigeonReport pending = reportWithStatus(FoundPigeonStatus.PENDING);
        when(foundPigeonRepository.findById(1L)).thenReturn(Optional.of(pending));
        stubSave();

        FoundPigeonDto approved = foundPigeonService.updateStatus(1L, FoundPigeonStatus.APPROVED);
        assertEquals(FoundPigeonStatus.APPROVED, approved.status());

        FoundPigeonDto resolved = foundPigeonService.updateStatus(1L, FoundPigeonStatus.RESOLVED);
        assertEquals(FoundPigeonStatus.RESOLVED, resolved.status());
    }

    @Test
    @DisplayName("Skipping the verification step is rejected")
    void rejectsTransitionFromPendingToResolved() {
        when(foundPigeonRepository.findById(1L))
                .thenReturn(Optional.of(reportWithStatus(FoundPigeonStatus.PENDING)));

        assertThrows(IllegalStateException.class,
                () -> foundPigeonService.updateStatus(1L, FoundPigeonStatus.RESOLVED));
        verify(foundPigeonRepository, never()).save(any());
    }

    @Test
    @DisplayName("Closed reports cannot be reopened")
    void rejectsReopeningClosedReports() {
        when(foundPigeonRepository.findById(1L))
                .thenReturn(Optional.of(reportWithStatus(FoundPigeonStatus.RESOLVED)));

        assertThrows(IllegalStateException.class,
                () -> foundPigeonService.updateStatus(1L, FoundPigeonStatus.APPROVED));

        when(foundPigeonRepository.findById(2L))
                .thenReturn(Optional.of(reportWithStatus(FoundPigeonStatus.REJECTED)));

        assertThrows(IllegalStateException.class,
                () -> foundPigeonService.updateStatus(2L, FoundPigeonStatus.APPROVED));
    }

    @Test
    @DisplayName("The administrator note can be added, edited and cleared")
    void managesAdminNote() {
        FoundPigeonReport report = reportWithStatus(FoundPigeonStatus.APPROVED);
        when(foundPigeonRepository.findById(1L)).thenReturn(Optional.of(report));
        stubSave();

        assertEquals("Właściciel ustalony.",
                foundPigeonService.updateAdminNote(1L, "Właściciel ustalony.").adminNote());

        assertEquals("Kontakt przekazany hodowcy.",
                foundPigeonService.updateAdminNote(1L, "Kontakt przekazany hodowcy.").adminNote());

        assertNull(foundPigeonService.updateAdminNote(1L, "   ").adminNote());
    }

    @Test
    @DisplayName("The search normalizes the ring number before querying")
    void normalizesSearchQuery() {
        when(foundPigeonRepository.search(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(reportWithStatus(FoundPigeonStatus.PENDING))));

        foundPigeonService.searchReports(FoundPigeonStatus.PENDING, "pl-0208", 0, 10);

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(foundPigeonRepository).search(eq(FoundPigeonStatus.PENDING), query.capture(), any(Pageable.class));

        assertEquals("PL0208", query.getValue());
    }

    @Test
    @DisplayName("A blank search phrase matches every report instead of being passed as null")
    void blankSearchMatchesEverything() {
        when(foundPigeonRepository.search(any(), any(), any())).thenReturn(Page.empty());

        foundPigeonService.searchReports(null, "   ", 0, 10);

        // An empty text, never null: PostgreSQL cannot infer the type of a null LIKE parameter
        // and rejects the query, while an empty pattern simply matches every ring number.
        verify(foundPigeonRepository).search(eq(null), eq(""), any(Pageable.class));
    }

    @Test
    @DisplayName("A missing search phrase is also passed as an empty text")
    void nullSearchIsPassedAsEmptyText() {
        when(foundPigeonRepository.search(any(), any(), any())).thenReturn(Page.empty());

        foundPigeonService.searchReports(null, null, 0, 10);

        verify(foundPigeonRepository).search(eq(null), eq(""), any(Pageable.class));
    }

    @Test
    @DisplayName("Pagination parameters outside the allowed range are rejected")
    void rejectsInvalidPaginationParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> foundPigeonService.searchReports(null, null, -1, 10));
        assertThrows(IllegalArgumentException.class,
                () -> foundPigeonService.searchReports(null, null, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> foundPigeonService.searchReports(null, null, 0, 51));

        verify(foundPigeonRepository, never()).search(any(), any(), any());
    }

    @Test
    @DisplayName("Reports are sorted from the newest submission")
    void sortsByNewestFirst() {
        when(foundPigeonRepository.search(any(), any(), any())).thenReturn(Page.empty());

        foundPigeonService.searchReports(null, null, 0, 10);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(foundPigeonRepository).search(any(), any(), pageable.capture());

        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(10, pageable.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC,
                pageable.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    @DisplayName("A missing report ends with a not-found error")
    void reportsMissingEntity() {
        when(foundPigeonRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> foundPigeonService.getReport(999L));
        assertThrows(EntityNotFoundException.class, () -> foundPigeonService.deleteReport(999L));
    }

    @Test
    @DisplayName("The administrator can delete a report together with the contact details")
    void deletesReport() {
        FoundPigeonReport report = reportWithStatus(FoundPigeonStatus.RESOLVED);
        when(foundPigeonRepository.findById(1L)).thenReturn(Optional.of(report));

        foundPigeonService.deleteReport(1L);

        verify(foundPigeonRepository).delete(report);
    }

    private FoundPigeonReport reportWithStatus(FoundPigeonStatus status) {
        FoundPigeonReport report = new FoundPigeonReport();
        report.setId(1L);
        report.setRingNumber("PL-0208-24-1234");
        report.setRingNumberNormalized("PL0208241234");
        report.setContactPhone("601234567");
        report.setPreferredLanguage(ReportLanguage.PL);
        report.setStatus(status);
        return report;
    }
}
