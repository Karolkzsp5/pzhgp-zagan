package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.NotificationDto;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.Notification;
import com.pzhgp.backend.entity.NotificationType;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final BreederRepository breederRepository;

    @Transactional
    public void createNotification(Long recipientId, String message, String link, NotificationType type) {
        Breeder recipient = breederRepository.findById(recipientId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono odbiorcy o ID: " + recipientId));

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setType(type);
        notification.setRead(false);

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(String userEmail) {
        Breeder breeder = getBreederByEmail(userEmail);
        return notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(breeder.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createBulkNotifications(List<Breeder> recipients, String message, String link, NotificationType type) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }

        List<Notification> notifications = recipients.stream().map(recipient -> {
            Notification notification = new Notification();
            notification.setRecipient(recipient);
            notification.setMessage(message);
            notification.setLink(link);
            notification.setType(type);
            notification.setRead(false);
            return notification;
        }).collect(Collectors.toList());

        notificationRepository.saveAll(notifications);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userEmail) {
        Breeder breeder = getBreederByEmail(userEmail);
        return notificationRepository.countByRecipientIdAndIsReadFalse(breeder.getId());
    }

    @Transactional
    public void markAsRead(Long notificationId, String userEmail) {
        Breeder breeder = getBreederByEmail(userEmail);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono powiadomienia."));

        if (!notification.getRecipient().getId().equals(breeder.getId())) {
            throw new IllegalStateException("Brak uprawnień do modyfikacji tego powiadomienia.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String userEmail) {
        Breeder breeder = getBreederByEmail(userEmail);
        notificationRepository.markAllAsReadByRecipientId(breeder.getId());
    }

    private Breeder getBreederByEmail(String email) {
        return breederRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika: " + email));
    }

    private NotificationDto mapToDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getMessage(),
                notification.getLink(),
                notification.isRead(),
                notification.getType().name(),
                notification.getCreatedAt()
        );
    }
}