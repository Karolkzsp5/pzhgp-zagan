package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.AnnouncementRequestDto;
import com.pzhgp.backend.dto.AnnouncementResponseDto;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.exception.ForbiddenOperationException;
import com.pzhgp.backend.repository.AnnouncementRepository;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.utils.PaginationUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AnnouncementRepository announcementRepository;
    private final BreederRepository breederRepository;
    private final NotificationService notificationService;

    @Transactional
    public void createAnnouncement(AnnouncementRequestDto requestDto, String authorEmail) {
        Breeder author = breederRepository.requireByEmail(authorEmail);

        if (author.getRole() == Role.BREEDER) {
            throw new ForbiddenOperationException("Zwykły hodowca nie może dodawać ogłoszeń.");
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(requestDto.title());
        announcement.setContent(requestDto.content());
        announcement.setPinned(requestDto.isPinned());
        announcement.setAuthor(author);

        announcementRepository.save(announcement);

        List<Breeder> recipients = breederRepository.findByStatus(AccountStatus.ACTIVE).stream()
                .filter(breeder -> !breeder.getId().equals(author.getId()))
                .toList();

        String notificationMessage = author.getFullName() + " dodał/a ogłoszenie na stronie głównej";

        notificationService.createBulkNotifications(
                recipients, notificationMessage, "/", NotificationType.NEW_ANNOUNCEMENT);
    }

    @Transactional
    public void updateAnnouncement(Long id, AnnouncementRequestDto requestDto, String userEmail) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono ogłoszenia o ID: " + id));
        Breeder editor = breederRepository.requireByEmail(userEmail);

        if (!canEditAnnouncement(announcement, editor)) {
            throw new ForbiddenOperationException(
                    "Brak uprawnień. Tylko autor ogłoszenia posiadający rolę administratora lub moderatora może je modyfikować.");
        }

        announcement.setTitle(requestDto.title());
        announcement.setContent(requestDto.content());
        announcement.setPinned(requestDto.isPinned());

        announcementRepository.save(announcement);
    }

    @Transactional
    public void deleteAnnouncement(Long id, String userEmail) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono ogłoszenia o ID: " + id));
        Breeder editor = breederRepository.requireByEmail(userEmail);

        if (!canDeleteAnnouncement(announcement, editor)) {
            throw new ForbiddenOperationException(editor.getRole() == Role.ADMINISTRATOR
                    ? "Administrator nie może usuwać ogłoszeń należących do innych administratorów."
                    : "Brak uprawnień do usunięcia tego ogłoszenia.");
        }

        boolean deletedByAdministrator = !announcement.getAuthor().getId().equals(editor.getId());
        if (deletedByAdministrator) {
            String message = String.format("Twoje ogłoszenie \"%s\" zostało usunięte przez administratora %s.",
                    announcement.getTitle(), editor.getFullName());

            notificationService.createNotification(
                    announcement.getAuthor(), message, "/", NotificationType.ANNOUNCEMENT_DELETED);
        }

        announcementRepository.delete(announcement);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementResponseDto> getAllAnnouncements(String currentUserEmail, int page, int size) {
        Breeder currentUser = currentUserEmail == null
                ? null
                : breederRepository.findByEmail(currentUserEmail).orElse(null);

        PaginationUtils.validate(page, size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);

        return announcementRepository.findAllByOrderByIsPinnedDescCreatedAtDescIdDesc(pageable)
                .map(announcement -> mapToDto(announcement, currentUser));
    }

    private boolean canEditAnnouncement(Announcement announcement, Breeder requester) {
        if (requester == null) {
            return false;
        }

        boolean hasEditorRole = requester.getRole() == Role.ADMINISTRATOR || requester.getRole() == Role.MODERATOR;
        return hasEditorRole && announcement.getAuthor().getId().equals(requester.getId());
    }

    /**
     * Autor może usunąć własne ogłoszenie, a administrator — ogłoszenie każdego,
     * kto nie jest administratorem.
     */
    private boolean canDeleteAnnouncement(Announcement announcement, Breeder requester) {
        if (requester == null) {
            return false;
        }

        boolean isAuthor = announcement.getAuthor().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ADMINISTRATOR;
        boolean isAuthorAdmin = announcement.getAuthor().getRole() == Role.ADMINISTRATOR;

        return isAuthor || (isAdmin && !isAuthorAdmin);
    }

    private AnnouncementResponseDto mapToDto(Announcement announcement, Breeder currentUser) {
        return new AnnouncementResponseDto(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getAuthor().getFullName(),
                announcement.isPinned(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt(),
                canEditAnnouncement(announcement, currentUser),
                canDeleteAnnouncement(announcement, currentUser)
        );
    }
}
