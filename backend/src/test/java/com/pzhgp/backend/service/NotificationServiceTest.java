package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.NotificationDto;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.Notification;
import com.pzhgp.backend.entity.NotificationType;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private BreederRepository breederRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Breeder testBreeder;
    private Breeder anotherBreeder;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testBreeder = new Breeder();
        testBreeder.setId(1L);
        testBreeder.setEmail("test@test.pl");

        anotherBreeder = new Breeder();
        anotherBreeder.setId(2L);
        anotherBreeder.setEmail("inny@test.pl");

        testNotification = new Notification();
        testNotification.setId(100L);
        testNotification.setRecipient(testBreeder);
        testNotification.setMessage("Testowa wiadomość");
        testNotification.setLink("/test");
        testNotification.setType(NotificationType.ACCOUNT_APPROVED);
        testNotification.setRead(false);
        testNotification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should successfully create a single notification")
    void shouldCreateNotification() {
        when(breederRepository.findById(1L)).thenReturn(Optional.of(testBreeder));

        assertDoesNotThrow(() -> notificationService.createNotification(
                1L, "Wiadomość", "/link", NotificationType.ROLE_CHANGED));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification savedNotification = captor.getValue();
        assertEquals("Wiadomość", savedNotification.getMessage());
        assertEquals("/link", savedNotification.getLink());
        assertEquals(NotificationType.ROLE_CHANGED, savedNotification.getType());
        assertFalse(savedNotification.isRead());
        assertEquals(testBreeder, savedNotification.getRecipient());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when creating notification for non-existent breeder")
    void shouldThrowExceptionWhenRecipientNotFound() {
        when(breederRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> notificationService.createNotification(99L, "Msg", null, NotificationType.NEW_ANNOUNCEMENT));

        assertEquals("Nie znaleziono odbiorcy o ID: 99", ex.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user email does not exist in DB")
    void shouldThrowExceptionWhenUserEmailNotFound() {
        when(breederRepository.findByEmail("unknown@test.pl")).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> notificationService.getUserNotifications("unknown@test.pl"));

        assertEquals("Nie znaleziono użytkownika: unknown@test.pl", ex.getMessage());
        verify(notificationRepository, never()).findAllByRecipientIdOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Should create bulk notifications using saveAll")
    void shouldCreateBulkNotifications() {
        List<Breeder> recipients = List.of(testBreeder, anotherBreeder);

        assertDoesNotThrow(() -> notificationService.createBulkNotifications(
                recipients, "Masowa wiadomość", "/", NotificationType.NEW_ANNOUNCEMENT));

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository, times(1)).saveAll(captor.capture());

        List<Notification> savedNotifications = captor.getValue();
        assertEquals(2, savedNotifications.size());
        assertEquals("Masowa wiadomość", savedNotifications.get(0).getMessage());
        assertEquals("Masowa wiadomość", savedNotifications.get(1).getMessage());
    }

    @Test
    @DisplayName("Should not call saveAll when recipient list is null")
    void shouldNotSaveBulkWhenListIsNull() {
        assertDoesNotThrow(() -> notificationService.createBulkNotifications(
                null, "Masowa wiadomość", "/", NotificationType.NEW_ANNOUNCEMENT));

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should not call saveAll when recipient list is empty")
    void shouldNotSaveBulkWhenListIsEmpty() {
        assertDoesNotThrow(() -> notificationService.createBulkNotifications(
                List.of(), "Masowa wiadomość", "/", NotificationType.NEW_ANNOUNCEMENT));

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should return user notifications mapped to DTOs")
    void shouldGetUserNotifications() {
        when(breederRepository.findByEmail("test@test.pl")).thenReturn(Optional.of(testBreeder));
        when(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(testNotification));

        List<NotificationDto> result = notificationService.getUserNotifications("test@test.pl");

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).id());
        assertEquals("Testowa wiadomość", result.get(0).message());
        assertFalse(result.get(0).isRead());
        assertEquals("ACCOUNT_APPROVED", result.get(0).type());
    }

    @Test
    @DisplayName("Should correctly return unread count")
    void shouldGetUnreadCount() {
        when(breederRepository.findByEmail("test@test.pl")).thenReturn(Optional.of(testBreeder));
        when(notificationRepository.countByRecipientIdAndIsReadFalse(1L)).thenReturn(5L);

        long count = notificationService.getUnreadCount("test@test.pl");

        assertEquals(5L, count);
        verify(notificationRepository, times(1)).countByRecipientIdAndIsReadFalse(1L);
    }

    @Test
    @DisplayName("Should successfully mark notification as read")
    void shouldMarkAsRead() {
        when(breederRepository.findByEmail("test@test.pl")).thenReturn(Optional.of(testBreeder));
        when(notificationRepository.findById(100L)).thenReturn(Optional.of(testNotification));

        assertDoesNotThrow(() -> notificationService.markAsRead(100L, "test@test.pl"));

        assertTrue(testNotification.isRead());
        verify(notificationRepository, times(1)).save(testNotification);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when marking non-existent notification as read")
    void shouldThrowExceptionWhenNotificationNotFound() {
        when(breederRepository.findByEmail("test@test.pl")).thenReturn(Optional.of(testBreeder));
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> notificationService.markAsRead(999L, "test@test.pl"));

        assertEquals("Nie znaleziono powiadomienia.", ex.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when marking someone else's notification as read")
    void shouldPreventMarkingOthersNotificationAsRead() {
        when(breederRepository.findByEmail("other@test.pl")).thenReturn(Optional.of(anotherBreeder));
        when(notificationRepository.findById(100L)).thenReturn(Optional.of(testNotification));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> notificationService.markAsRead(100L, "other@test.pl"));

        assertEquals("Brak uprawnień do modyfikacji tego powiadomienia.", ex.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully trigger markAllAsReadByRecipientId in repository")
    void shouldMarkAllAsRead() {
        when(breederRepository.findByEmail("test@test.pl")).thenReturn(Optional.of(testBreeder));

        assertDoesNotThrow(() -> notificationService.markAllAsRead("test@test.pl"));

        verify(notificationRepository, times(1)).markAllAsReadByRecipientId(1L);
    }
}