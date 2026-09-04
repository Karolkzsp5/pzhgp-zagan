package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.AnnouncementRequestDto;
import com.pzhgp.backend.dto.AnnouncementResponseDto;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.AnnouncementRepository;
import com.pzhgp.backend.repository.BreederRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private BreederRepository breederRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AnnouncementService announcementService;

    private Breeder admin1;
    private Breeder admin2;
    private Breeder moderator;
    private Breeder standardBreeder;
    private Announcement announcement;

    @BeforeEach
    void setUp() {
        admin1 = new Breeder();
        admin1.setId(1L);
        admin1.setEmail("admin1@test.pl");
        admin1.setRole(Role.ADMINISTRATOR);
        admin1.setName("Jan");
        admin1.setSurname("Kowalski");

        admin2 = new Breeder();
        admin2.setId(2L);
        admin2.setEmail("admin2@test.pl");
        admin2.setRole(Role.ADMINISTRATOR);

        moderator = new Breeder();
        moderator.setId(3L);
        moderator.setEmail("moderator@test.pl");
        moderator.setRole(Role.MODERATOR);
        moderator.setName("Anna");
        moderator.setSurname("Nowak");

        standardBreeder = new Breeder();
        standardBreeder.setId(4L);
        standardBreeder.setEmail("hodowca@test.pl");
        standardBreeder.setRole(Role.BREEDER);
        standardBreeder.setStatus(AccountStatus.ACTIVE);

        announcement = new Announcement();
        announcement.setId(10L);
        announcement.setTitle("Test title");
        announcement.setContent("Test content");
        announcement.setAuthor(moderator);
        announcement.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should throw exception when a standard breeder tries to create an announcement")
    void createAnnouncement_ShouldThrowException_WhenUserIsBreeder() {
        AnnouncementRequestDto request = new AnnouncementRequestDto("Tytuł", "Treść", false);
        when(breederRepository.findByEmail(standardBreeder.getEmail())).thenReturn(Optional.of(standardBreeder));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                announcementService.createAnnouncement(request, standardBreeder.getEmail())
        );
        assertEquals("Zwykły hodowca nie może dodawać ogłoszeń.", exception.getMessage());
        verify(announcementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should save announcement and send notifications (excluding author) when the author is an administrator")
    void createAnnouncement_ShouldSaveAndNotify_WhenUserIsAdmin() {
        AnnouncementRequestDto request = new AnnouncementRequestDto("Ważny lot", "Szczegóły", true);
        when(breederRepository.findByEmail(admin1.getEmail())).thenReturn(Optional.of(admin1));
        when(breederRepository.findByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(admin1, standardBreeder));

        announcementService.createAnnouncement(request, admin1.getEmail());

        verify(announcementRepository, times(1)).save(any(Announcement.class));
        verify(notificationService, times(1)).createBulkNotifications(
                argThat(list -> list.size() == 1 && list.contains(standardBreeder)),
                eq("Jan Kowalski dodał/a ogłoszenie na stronie głównej"),
                eq("/"),
                eq(NotificationType.NEW_ANNOUNCEMENT)
        );
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when author is not found in the database")
    void createAnnouncement_ShouldThrowException_WhenAuthorNotFound() {
        AnnouncementRequestDto request = new AnnouncementRequestDto("Tytuł", "Treść", false);
        String unknownEmail = "ghost@test.pl";
        when(breederRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                announcementService.createAnnouncement(request, unknownEmail)
        );
        assertEquals("Nie znaleziono autora.", exception.getMessage());
        verify(announcementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully update announcement when user is the author")
    void updateAnnouncement_ShouldUpdate_WhenUserIsAuthor() {
        AnnouncementRequestDto request = new AnnouncementRequestDto("Zaktualizowany tytuł", "Nowa treść", true);
        when(announcementRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(breederRepository.findByEmail(moderator.getEmail())).thenReturn(Optional.of(moderator));

        announcementService.updateAnnouncement(announcement.getId(), request, moderator.getEmail());

        assertEquals("Zaktualizowany tytuł", announcement.getTitle());
        assertEquals("Nowa treść", announcement.getContent());
        assertTrue(announcement.isPinned());
        verify(announcementRepository, times(1)).save(announcement);
    }

    @Test
    @DisplayName("Should throw exception when administrator tries to update moderator's announcement (edit restricted to author only)")
    void updateAnnouncement_ShouldThrowException_WhenAdminTriesToUpdateModPost() {
        AnnouncementRequestDto request = new AnnouncementRequestDto("Zmieniony", "Treść", false);
        when(announcementRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(breederRepository.findByEmail(admin1.getEmail())).thenReturn(Optional.of(admin1));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                announcementService.updateAnnouncement(announcement.getId(), request, admin1.getEmail())
        );
        assertEquals("Brak uprawnień. Tylko autor ogłoszenia może je modyfikować.", exception.getMessage());
        verify(announcementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully delete announcement when user is the author")
    void deleteAnnouncement_ShouldDelete_WhenUserIsAuthor() {
        when(announcementRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(breederRepository.findByEmail(moderator.getEmail())).thenReturn(Optional.of(moderator));

        announcementService.deleteAnnouncement(announcement.getId(), moderator.getEmail());

        verify(announcementRepository, times(1)).delete(announcement);
        verify(notificationService, never()).createBulkNotifications(any(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should successfully delete announcement and notify the author when administrator deletes moderator's post")
    void deleteAnnouncement_ShouldDeleteAndNotify_WhenAdminDeletesModPost() {
        when(announcementRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(breederRepository.findByEmail(admin1.getEmail())).thenReturn(Optional.of(admin1));

        announcementService.deleteAnnouncement(announcement.getId(), admin1.getEmail());

        verify(announcementRepository, times(1)).delete(announcement);
        verify(notificationService, times(1)).createBulkNotifications(
                argThat(list -> list.size() == 1 && list.contains(moderator)),
                eq("Twoje ogłoszenie \"Test title\" zostało usunięte przez administratora Jan Kowalski."),
                eq("/"),
                eq(NotificationType.ANNOUNCEMENT_DELETED)
        );
    }

    @Test
    @DisplayName("Should throw exception when administrator tries to delete another administrator's announcement")
    void deleteAnnouncement_ShouldThrowException_WhenAdminDeletesAdminPost() {
        announcement.setAuthor(admin1);
        when(announcementRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(breederRepository.findByEmail(admin2.getEmail())).thenReturn(Optional.of(admin2));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                announcementService.deleteAnnouncement(announcement.getId(), admin2.getEmail())
        );
        assertEquals("Administrator nie może usuwać ogłoszeń należących do innych administratorów.", exception.getMessage());
        verify(announcementRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should set both canEdit and canDelete flags to true when user is the author (Moderator)")
    void getAllAnnouncements_ShouldSetBothFlagsTrue_WhenUserIsAuthor() {
        when(breederRepository.findByEmail(moderator.getEmail())).thenReturn(Optional.of(moderator));
        when(announcementRepository.findAllByOrderByIsPinnedDescCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(announcement)));

        Page<AnnouncementResponseDto> result = announcementService.getAllAnnouncements(moderator.getEmail(), 0, 10);

        assertEquals(1, result.getTotalElements());
        AnnouncementResponseDto dto = result.getContent().getFirst();
        assertTrue(dto.canEdit(), "Autor (moderator) powinien móc edytować swoje ogłoszenie");
        assertTrue(dto.canDelete(), "Autor (moderator) powinien móc usunąć swoje ogłoszenie");
    }

    @Test
    @DisplayName("Should set both canEdit and canDelete flags to true when user is the author (Administrator)")
    void getAllAnnouncements_ShouldSetBothFlagsTrue_WhenAdminIsAuthor() {
        announcement.setAuthor(admin1);
        when(breederRepository.findByEmail(admin1.getEmail())).thenReturn(Optional.of(admin1));
        when(announcementRepository.findAllByOrderByIsPinnedDescCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(announcement)));

        Page<AnnouncementResponseDto> result = announcementService.getAllAnnouncements(admin1.getEmail(), 0, 10);

        assertEquals(1, result.getTotalElements());
        AnnouncementResponseDto dto = result.getContent().getFirst();
        assertTrue(dto.canEdit(), "Autor (admin) powinien móc edytować swoje ogłoszenie");
        assertTrue(dto.canDelete(), "Autor (admin) powinien móc usunąć swoje ogłoszenie");
    }

    @Test
    @DisplayName("Should set canDelete=true and canEdit=false flags for administrator viewing moderator's post")
    void getAllAnnouncements_ShouldSetCorrectFlags_WhenUserIsAdmin() {
        when(breederRepository.findByEmail(admin1.getEmail())).thenReturn(Optional.of(admin1));
        when(announcementRepository.findAllByOrderByIsPinnedDescCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(announcement)));

        Page<AnnouncementResponseDto> result = announcementService.getAllAnnouncements(admin1.getEmail(), 0, 10);

        assertEquals(1, result.getTotalElements());
        AnnouncementResponseDto dto = result.getContent().getFirst();
        assertFalse(dto.canEdit(), "Admin nie powinien mieć prawa edycji cudzego posta");
        assertTrue(dto.canDelete(), "Admin powinien mieć prawo usunięcia posta moderatora");
    }

    @Test
    @DisplayName("Should set false flags for both actions when user is anonymous")
    void getAllAnnouncements_ShouldSetCorrectFlags_WhenUserIsAnonymous() {
        when(announcementRepository.findAllByOrderByIsPinnedDescCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(announcement)));

        Page<AnnouncementResponseDto> result = announcementService.getAllAnnouncements(null, 0, 10);

        assertEquals(1, result.getTotalElements());
        AnnouncementResponseDto dto = result.getContent().getFirst();
        assertFalse(dto.canEdit(), "Niezalogowany użytkownik nie może edytować");
        assertFalse(dto.canDelete(), "Niezalogowany użytkownik nie może usuwać");
    }

    @Test
    @DisplayName("Should set false flags for both actions when moderator views someone else's post")
    void getAllAnnouncements_ShouldSetBothFlagsFalse_WhenUserIsModeratorAndNotAuthor() {
        announcement.setAuthor(admin1);
        when(breederRepository.findByEmail(moderator.getEmail())).thenReturn(Optional.of(moderator));
        when(announcementRepository.findAllByOrderByIsPinnedDescCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(announcement)));

        Page<AnnouncementResponseDto> result = announcementService.getAllAnnouncements(moderator.getEmail(), 0, 10);

        assertEquals(1, result.getTotalElements());
        AnnouncementResponseDto dto = result.getContent().getFirst();
        assertFalse(dto.canEdit(), "Moderator nie powinien móc edytować cudzego ogłoszenia");
        assertFalse(dto.canDelete(), "Moderator nie powinien móc usuwać cudzego ogłoszenia");
    }
}