package com.pzhgp.backend.controller;

import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.NotificationRepository;
import com.pzhgp.backend.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    private Breeder testBreeder;
    private Breeder anotherBreeder;
    private Notification unreadNotification;
    private Notification readNotification;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        breederRepository.deleteAll();
        sectionRepository.deleteAll();

        Section section = new Section(null, "Test Section", 1);
        section = sectionRepository.save(section);

        testBreeder = new Breeder();
        testBreeder.setEmail("test@test.pl");
        testBreeder.setPasswordHash("$2a$12$R1NIIhVVnGXVo5KH0hKSze1J.d5OI5sHAd0to9..rzta3I.OrwmZW");
        testBreeder.setName("Jan");
        testBreeder.setSurname("Kowalski");
        testBreeder.setPhoneNumber("123456789");
        testBreeder.setDateOfBirth(LocalDate.of(1990, 1, 1));
        testBreeder.setSection(section);
        testBreeder.setStatus(AccountStatus.ACTIVE);
        testBreeder.setRole(Role.BREEDER);
        testBreeder.setCreatedAt(LocalDateTime.now());
        testBreeder = breederRepository.save(testBreeder);

        anotherBreeder = new Breeder();
        anotherBreeder.setEmail("inny@test.pl");
        anotherBreeder.setPasswordHash("$2a$12$R1NIIhVVnGXVo5KH0hKSze1J.d5OI5sHAd0to9..rzta3I.OrwmZW");
        anotherBreeder.setName("Piotr");
        anotherBreeder.setSurname("Nowak");
        anotherBreeder.setPhoneNumber("987654321");
        anotherBreeder.setDateOfBirth(LocalDate.of(1995, 5, 5));
        anotherBreeder.setSection(section);
        anotherBreeder.setStatus(AccountStatus.ACTIVE);
        anotherBreeder.setRole(Role.BREEDER);
        anotherBreeder.setCreatedAt(LocalDateTime.now());
        breederRepository.save(anotherBreeder);

        unreadNotification = new Notification();
        unreadNotification.setRecipient(testBreeder);
        unreadNotification.setMessage("Nowa nieprzeczytana wiadomość");
        unreadNotification.setLink("/link-1");
        unreadNotification.setType(NotificationType.ACCOUNT_APPROVED);
        unreadNotification.setRead(false);

        readNotification = new Notification();
        readNotification.setRecipient(testBreeder);
        readNotification.setMessage("Stara przeczytana wiadomość");
        readNotification.setLink("/link-2");
        readNotification.setType(NotificationType.ROLE_CHANGED);
        readNotification.setRead(true);

        notificationRepository.saveAll(List.of(unreadNotification, readNotification));
    }

    @Test
    @DisplayName("Should return 403 for unauthenticated users")
    void shouldForbidAccessForUnauthenticatedUsers() throws Exception {
        mockMvc.perform(get("/api/notifications")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return user notifications mapped to DTOs with all fields")
    void shouldReturnUserNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .with(user("test@test.pl")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.message == 'Nowa nieprzeczytana wiadomość')].link").value("/link-1"))
                .andExpect(jsonPath("$[?(@.message == 'Nowa nieprzeczytana wiadomość')].type").value("ACCOUNT_APPROVED"))
                .andExpect(jsonPath("$[?(@.message == 'Nowa nieprzeczytana wiadomość')].isRead").value(false))
                .andExpect(jsonPath("$[?(@.message == 'Stara przeczytana wiadomość')].link").value("/link-2"))
                .andExpect(jsonPath("$[?(@.message == 'Stara przeczytana wiadomość')].type").value("ROLE_CHANGED"))
                .andExpect(jsonPath("$[?(@.message == 'Stara przeczytana wiadomość')].isRead").value(true));
    }

    @Test
    @DisplayName("Should return correct unread count")
    void shouldReturnUnreadCount() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(user("test@test.pl")))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("Should successfully mark notification as read and decrease unread count")
    void shouldMarkNotificationAsReadAndDecreaseCount() throws Exception {
        Long notificationId = unreadNotification.getId();

        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(user("test@test.pl")))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        mockMvc.perform(put("/api/notifications/" + notificationId + "/read")
                        .with(user("test@test.pl")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(user("test@test.pl")))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));

        Notification updatedNotification = notificationRepository.findById(notificationId).orElseThrow();
        assertTrue(updatedNotification.isRead());
    }

    @Test
    @DisplayName("Should trigger custom @Modifying query and mark all as read")
    void shouldMarkAllNotificationsAsRead() throws Exception {
        Notification extraUnread = new Notification();
        extraUnread.setRecipient(testBreeder);
        extraUnread.setMessage("Kolejne nieprzeczytane");
        extraUnread.setType(NotificationType.NEW_ANNOUNCEMENT);
        extraUnread.setRead(false);
        notificationRepository.save(extraUnread);

        mockMvc.perform(put("/api/notifications/read-all")
                        .with(user("test@test.pl")))
                .andExpect(status().isOk());

        List<Notification> allNotifications = notificationRepository.findAll();
        for (Notification n : allNotifications) {
            assertTrue(n.isRead(), "Powiadomienie o ID " + n.getId() + " nie zostało oznaczone jako przeczytane!");
        }
    }

    @Test
    @DisplayName("Should prevent IDOR and return error when marking someone else's notification as read")
    void shouldPreventIdorWhenMarkingOthersNotificationAsRead() throws Exception {
        Long othersNotificationId = unreadNotification.getId();

        mockMvc.perform(put("/api/notifications/" + othersNotificationId + "/read")
                        .with(user("inny@test.pl")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 404 Not Found when marking non-existent notification as read")
    void shouldReturn404WhenNotificationNotFound() throws Exception {
        Long nonExistentId = 999999L;

        mockMvc.perform(put("/api/notifications/" + nonExistentId + "/read")
                        .with(user("test@test.pl")))
                .andExpect(status().isNotFound());
    }
}