package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.AnnouncementRequestDto;
import com.pzhgp.backend.dto.AnnouncementResponseDto;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.AnnouncementRepository;
import com.pzhgp.backend.repository.BreederRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
    public void createAnnouncement(AnnouncementRequestDto requestDto, String authorEmail) {
        Breeder author = breederRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono autora."));

        if (author.getRole() == Role.BREEDER) {
            throw new IllegalStateException("Zwykły hodowca nie może dodawać ogłoszeń.");
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(requestDto.title());
        announcement.setContent(requestDto.content());
        announcement.setPinned(requestDto.isPinned());
        announcement.setAuthor(author);

        announcementRepository.save(announcement);

        List<Breeder> activeBreeders = breederRepository.findByStatus(AccountStatus.ACTIVE);
        List<Breeder> recipients = activeBreeders.stream()
                .filter(b -> !b.getId().equals(author.getId()))
                .collect(Collectors.toList());

        String authorFullName = author.getName() + " " + author.getSurname();
        String notificationMessage = "Hodowca " + authorFullName + " dodał ogłoszenie na stronie głównej";

        notificationService.createBulkNotifications(
                recipients,
                notificationMessage,
                "/",
                NotificationType.NEW_ANNOUNCEMENT
        );
    }

    @Transactional
    public void updateAnnouncement(Long id, AnnouncementRequestDto requestDto, String userEmail) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono ogłoszenia o ID: " + id));

        Breeder editor = breederRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono edytującego."));

        boolean isAuthor = announcement.getAuthor().getId().equals(editor.getId());
        boolean isAdmin = editor.getRole() == Role.ADMINISTRATOR;

        if (!isAuthor && !isAdmin) {
            throw new IllegalStateException("Brak uprawnień do edycji tego ogłoszenia.");
        }

        announcement.setTitle(requestDto.title());
        announcement.setContent(requestDto.content());
        announcement.setPinned(requestDto.isPinned());

        announcementRepository.save(announcement);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponseDto> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByIsPinnedDescCreatedAtDesc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AnnouncementResponseDto mapToDto(Announcement announcement) {
        return new AnnouncementResponseDto(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getAuthor().getName() + " " + announcement.getAuthor().getSurname(),
                announcement.isPinned(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}