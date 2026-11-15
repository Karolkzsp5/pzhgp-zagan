package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.NotificationDto;
import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.Notification;
import com.pzhgp.backend.entity.NotificationType;
import com.pzhgp.backend.entity.Section;
import com.pzhgp.backend.exception.ForbiddenOperationException;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.NotificationRepository;
import com.pzhgp.backend.utils.PaginationUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class NotificationService {

    /** Największa dopuszczalna liczba powiadomień na stronie. */
    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final BreederRepository breederRepository;

    @Transactional
    public void createNotification(Long recipientId, String message, String link, NotificationType type) {
        Breeder recipient = breederRepository.findById(recipientId).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono odbiorcy o ID: " + recipientId));

        notificationRepository.save(buildNotification(recipient, message, link, type));
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(String userEmail, int page, int size) {
        PaginationUtils.validate(page, size, MAX_PAGE_SIZE);
        Breeder breeder = breederRepository.requireByEmail(userEmail);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        return notificationRepository.findByRecipientId(breeder.getId(), pageable).map(this::mapToDto);
    }

    @Transactional
    public void createBulkNotifications(List<Breeder> recipients, String message, String link, NotificationType type) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }

        List<Notification> notifications = recipients.stream().map(recipient -> buildNotification(recipient, message, link, type)).toList();

        notificationRepository.saveAll(notifications);
    }

    /** Powiadamia wszystkich aktywnych hodowców oprócz autora zmiany. */
    @Transactional
    public void notifyActiveBreeders(Breeder author, String message, String link, NotificationType type) {
        notifyActiveBreeders(author, message, link, type, breeder -> true);
    }

    /** Powiadamia aktywnych hodowców z podanej sekcji oprócz autora zmiany. */
    @Transactional
    public void notifyActiveSectionMembers(Breeder author, Section section, String message, String link, NotificationType type) {
        notifyActiveBreeders(author, message, link, type, breeder -> breeder.getSection().getId().equals(section.getId()));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userEmail) {
        Breeder breeder = breederRepository.requireByEmail(userEmail);
        return notificationRepository.countByRecipientIdAndIsReadFalse(breeder.getId());
    }

    @Transactional
    public void markAsRead(Long notificationId, String userEmail) {
        Breeder breeder = breederRepository.requireByEmail(userEmail);
        Notification notification = findNotification(notificationId);

        if (!notification.getRecipient().getId().equals(breeder.getId())) {
            throw new ForbiddenOperationException("Brak uprawnień do modyfikacji tego powiadomienia.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String userEmail) {
        Breeder breeder = breederRepository.requireByEmail(userEmail);
        notificationRepository.markAllAsReadByRecipientId(breeder.getId());
    }

    @Transactional
    public void deleteNotification(Long notificationId, String userEmail) {
        Breeder breeder = breederRepository.requireByEmail(userEmail);
        Notification notification = findNotification(notificationId);

        if (!notification.getRecipient().getId().equals(breeder.getId())) {
            throw new ForbiddenOperationException("Brak uprawnień do usunięcia tego powiadomienia.");
        }

        notificationRepository.delete(notification);
    }

    private void notifyActiveBreeders(Breeder author, String message, String link, NotificationType type, Predicate<Breeder> recipientFilter) {
        List<Breeder> recipients = breederRepository.findByStatus(AccountStatus.ACTIVE).stream()
                .filter(breeder -> !breeder.getId().equals(author.getId()))
                .filter(recipientFilter)
                .toList();

        createBulkNotifications(recipients, message, link, type);
    }

    private Notification buildNotification(Breeder recipient, String message, String link, NotificationType type) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setType(type);
        notification.setRead(false);
        return notification;
    }

    private Notification findNotification(Long notificationId) {
        return notificationRepository.findById(notificationId).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono powiadomienia."));
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