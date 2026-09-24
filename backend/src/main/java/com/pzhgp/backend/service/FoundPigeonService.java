package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.FoundPigeonDto;
import com.pzhgp.backend.dto.FoundPigeonRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FoundPigeonRepository;
import com.pzhgp.backend.utils.PaginationUtils;
import com.pzhgp.backend.utils.RingNumberNormalizer;
import com.pzhgp.backend.exception.SubmissionRateLimitException;
import com.pzhgp.backend.exception.InvalidStatusTransitionException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Obsługa zgłoszeń odnalezienia gołębi.
 * <p>
 * Zgłoszenie trafia do systemu z publicznego formularza, bez logowania. Dane kontaktowe
 * znalazcy są danymi osobowymi: nie pojawiają się w logach, w powiadomieniach ani w żadnej
 * odpowiedzi publicznego endpointu — widzi je wyłącznie administrator w swoim panelu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoundPigeonService {

    /** Największa dopuszczalna liczba zgłoszeń na stronie listy administracyjnej. */
    private static final int MAX_PAGE_SIZE = 50;

    private final FoundPigeonRepository foundPigeonRepository;
    private final BreederRepository breederRepository;
    private final NotificationService notificationService;
    private final SubmissionRateLimiter rateLimiter;

    /**
     * Zapisuje nowe zgłoszenie i powiadamia administratorów.
     *
     * @param request  dane z formularza
     * @param clientKey adres zdalny nadawcy, wykorzystywany do ograniczania częstotliwości
     * @return identyfikator utworzonego zgłoszenia
     */
    @Transactional
    public Long createReport(FoundPigeonRequest request, String clientKey) {
        if (!rateLimiter.tryAcquire(clientKey)) {
            throw new SubmissionRateLimitException(
                    "Zbyt wiele zgłoszeń. Spróbuj ponownie później.");
        }

        FoundPigeonReport report = new FoundPigeonReport();
        report.setRingNumber(request.ringNumber().trim());
        report.setRingNumberNormalized(RingNumberNormalizer.normalize(request.ringNumber()));
        report.setContactPhone(trimToNull(request.contactPhone()));
        report.setContactEmail(trimToNull(request.contactEmail()));
        report.setFoundLocation(trimToNull(request.foundLocation()));
        report.setFoundCountry(trimToNull(request.foundCountry()));
        report.setDescription(trimToNull(request.description()));
        report.setPreferredLanguage(
                request.preferredLanguage() == null ? ReportLanguage.PL : request.preferredLanguage());
        report.setStatus(FoundPigeonStatus.PENDING);

        FoundPigeonReport saved = foundPigeonRepository.save(report);

        notifyAdministrators(saved);

        // W logu wyłącznie identyfikatory — żadnych danych kontaktowych znalazcy.
        log.info("Przyjęto zgłoszenie odnalezienia gołębia (ID: {}, obrączka: {}).",
                saved.getId(), saved.getRingNumber());

        return saved.getId();
    }

    /**
     * Lista zgłoszeń dla panelu administratora, od najnowszego.
     *
     * @param status     opcjonalny filtr statusu
     * @param ringNumber opcjonalny fragment numeru obrączki
     */
    @Transactional(readOnly = true)
    public Page<FoundPigeonDto> searchReports(FoundPigeonStatus status, String ringNumber,
                                              int page, int size) {
        PaginationUtils.validate(page, size, MAX_PAGE_SIZE);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Fraza wyszukiwania przechodzi przez tę samą normalizację co zapisany numer,
        // dzięki czemu "pl 0208" odnajduje zgłoszenie zapisane jako "PL-0208-24-1234".
        // Brak frazy daje pusty tekst, a nie null — patrz komentarz przy zapytaniu repozytorium.
        String normalizedQuery = RingNumberNormalizer.normalize(ringNumber);

        return foundPigeonRepository.search(status, normalizedQuery, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public FoundPigeonDto getReport(Long id) {
        return mapToDto(findReport(id));
    }

    /**
     * Zmienia status zgłoszenia, pilnując dozwolonej kolejności przejść.
     *
     * @throws IllegalStateException gdy przejście nie jest dozwolone
     */
    @Transactional
    public FoundPigeonDto updateStatus(Long id, FoundPigeonStatus newStatus) {
        FoundPigeonReport report = findReport(id);

        if (!report.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(String.format(
                    "Nie można zmienić statusu z %s na %s.",
                    report.getStatus(),
                    newStatus
            ));
        }

        report.setStatus(newStatus);
        FoundPigeonReport saved = foundPigeonRepository.save(report);

        log.info("Zmieniono status zgłoszenia ID {} na {}.", id, newStatus);

        return mapToDto(saved);
    }

    /** Zapisuje prywatną notatkę administratora; pusta wartość ją czyści. */
    @Transactional
    public FoundPigeonDto updateAdminNote(Long id, String adminNote) {
        FoundPigeonReport report = findReport(id);
        report.setAdminNote(trimToNull(adminNote));

        return mapToDto(foundPigeonRepository.save(report));
    }

    /** Usuwa zgłoszenie razem z danymi kontaktowymi, gdy przestaje być potrzebne. */
    @Transactional
    public void deleteReport(Long id) {
        FoundPigeonReport report = findReport(id);
        foundPigeonRepository.delete(report);

        log.info("Usunięto zgłoszenie odnalezienia gołębia o ID {}.", id);
    }

    /** Liczba zgłoszeń oczekujących na weryfikację — odznaka w panelu administratora. */
    @Transactional(readOnly = true)
    public long countPendingReports() {
        return foundPigeonRepository.countByStatus(FoundPigeonStatus.PENDING);
    }

    /**
     * Powiadamia aktywnych administratorów o nowym zgłoszeniu.
     * <p>
     * Powiadomienie zawiera wyłącznie numer obrączki i odnośnik do panelu — dane kontaktowe
     * znalazcy pozostają w bazie i są widoczne dopiero po otwarciu szczegółów zgłoszenia.
     * Moderatorzy i zwykli hodowcy nie dostają tych powiadomień.
     */
    private void notifyAdministrators(FoundPigeonReport report) {
        List<Breeder> administrators =
                breederRepository.findByRoleAndStatus(Role.ADMINISTRATOR, AccountStatus.ACTIVE);

        if (administrators.isEmpty()) {
            return;
        }

        String message = "Nowe zgłoszenie odnalezienia gołębia o numerze obrączki "
                + report.getRingNumber() + ".";

        notificationService.createBulkNotifications(
                administrators,
                message,
                "/found-pigeons/admin",
                NotificationType.NEW_FOUND_PIGEON_REPORT
        );
    }

    private FoundPigeonReport findReport(Long id) {
        return foundPigeonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono zgłoszenia o ID: " + id));
    }

    private FoundPigeonDto mapToDto(FoundPigeonReport report) {
        return new FoundPigeonDto(
                report.getId(),
                report.getRingNumber(),
                report.getContactPhone(),
                report.getContactEmail(),
                report.getFoundLocation(),
                report.getFoundCountry(),
                report.getDescription(),
                report.getPreferredLanguage(),
                report.getStatus(),
                report.getAdminNote(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
