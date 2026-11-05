package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.AnnouncementDto;
import com.pzhgp.backend.dto.AnnouncementRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.exception.ForbiddenOperationException;
import com.pzhgp.backend.repository.AnnouncementRepository;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.utils.PaginationUtils;
import com.pzhgp.backend.utils.RichTextSanitizer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final BreederRepository breederRepository;
    private final NotificationService notificationService;

    @Transactional
    public void createAnnouncement(AnnouncementRequest requestDto, String authorEmail) {
        Breeder author = breederRepository.requireByEmail(authorEmail);

        if (author.getRole() == Role.BREEDER) {
            throw new ForbiddenOperationException("Zwykły hodowca nie może dodawać ogłoszeń.");
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(requestDto.title());
        announcement.setContent(RichTextSanitizer.sanitize(requestDto.content()));
        announcement.setPinned(requestDto.isPinned());
        announcement.setAuthor(author);

        announcementRepository.save(announcement);

        List<Breeder> activeBreeders = breederRepository.findByStatus(AccountStatus.ACTIVE);
        List<Breeder> recipients = activeBreeders.stream().filter(b -> !b.getId().equals(author.getId())).collect(Collectors.toList());

        String notificationMessage = author.getFullName() + " dodał/a ogłoszenie na stronie głównej";

        notificationService.createBulkNotifications(recipients, notificationMessage, "/", NotificationType.NEW_ANNOUNCEMENT);
    }

    @Transactional
    public void updateAnnouncement(Long id, AnnouncementRequest requestDto, String userEmail) {
        Announcement announcement = announcementRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono ogłoszenia o ID: " + id));
        Breeder editor = breederRepository.requireByEmail(userEmail);

        if (!canEditAnnouncement(announcement, editor)) {
            throw new ForbiddenOperationException("Brak uprawnień. Tylko autor ogłoszenia posiadający rolę administratora lub moderatora może je modyfikować.");
        }

        announcement.setTitle(requestDto.title());
        announcement.setContent(RichTextSanitizer.sanitize(requestDto.content()));
        announcement.setPinned(requestDto.isPinned());

        announcementRepository.save(announcement);
    }

    @Transactional
    public void deleteAnnouncement(Long id, String userEmail) {
        Announcement announcement = announcementRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono ogłoszenia o ID: " + id));
        Breeder editor = breederRepository.requireByEmail(userEmail);

        if (!canDeleteAnnouncement(announcement, editor)) {
            if (editor.getRole() != Role.ADMINISTRATOR) {
                throw new ForbiddenOperationException("Brak uprawnień do usunięcia tego ogłoszenia.");
            }
            throw new ForbiddenOperationException("Administrator nie może usuwać ogłoszeń należących do innych administratorów.");
        }

        // Cudze ogłoszenie może usunąć tylko administrator, więc autor dostaje powiadomienie.
        if (!announcement.getAuthor().getId().equals(editor.getId())) {
            String message = String.format("Twoje ogłoszenie \"%s\" zostało usunięte przez administratora %s.", announcement.getTitle(), editor.getFullName());

            notificationService.createBulkNotifications(List.of(announcement.getAuthor()), message, "/", NotificationType.ANNOUNCEMENT_DELETED);
        }

        announcementRepository.delete(announcement);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementDto> getAllAnnouncements(String currentUserEmail, int page, int size) {
        Breeder currentUser = null;
        if (currentUserEmail != null) {
            currentUser = breederRepository.findByEmail(currentUserEmail).orElse(null);
        }

        Breeder finalCurrentUser = currentUser;
        PaginationUtils.validate(page, size, 100);
        Pageable pageable = PageRequest.of(page, size);

        return announcementRepository.findAllByOrderByIsPinnedDescCreatedAtDescIdDesc(pageable).map(announcement -> mapToDto(announcement, finalCurrentUser));
    }

    private boolean canEditAnnouncement(Announcement announcement, Breeder requester) {
        if (requester == null) {
            return false;
        }

        boolean hasEditorRole = requester.getRole() == Role.ADMINISTRATOR || requester.getRole() == Role.MODERATOR;
        return hasEditorRole && announcement.getAuthor().getId().equals(requester.getId());
    }

    // Autor może usunąć własne ogłoszenie, a administrator ogłoszenie każdego, kto nie jest administratorem.
    private boolean canDeleteAnnouncement(Announcement announcement, Breeder requester) {
        if (requester == null) {
            return false;
        }

        boolean isAuthor = announcement.getAuthor().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ADMINISTRATOR;
        boolean isAuthorAdmin = announcement.getAuthor().getRole() == Role.ADMINISTRATOR;

        return isAuthor || (isAdmin && !isAuthorAdmin);
    }

    private AnnouncementDto mapToDto(Announcement announcement, Breeder currentUser) {
        boolean canEdit = canEditAnnouncement(announcement, currentUser);
        boolean canDelete = canDeleteAnnouncement(announcement, currentUser);

        return new AnnouncementDto(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getAuthor().getFullName(),
                announcement.isPinned(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt(),
                canEdit,
                canDelete
        );
    }
}