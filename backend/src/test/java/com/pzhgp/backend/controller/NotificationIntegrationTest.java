package com.pzhgp.backend.controller;

import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.NotificationRepository;
import com.pzhgp.backend.repository.SectionRepository;
import com.pzhgp.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Notifications Integration Tests")
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private JwtService jwtService;

    private Breeder testBreeder;
    private Breeder anotherBreeder;
    private Notification unreadNotification;
    private Notification readNotification;

    private String testBreederToken;
    private String anotherBreederToken;

    @BeforeEach
    void setUp() {
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

        testBreederToken = jwtService.generateToken(testBreeder);

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
        anotherBreeder = breederRepository.save(anotherBreeder);

        anotherBreederToken = jwtService.generateToken(anotherBreeder);

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
    @DisplayName("Should return 401 when an unauthenticated user accesses notifications")
    void shouldRequireAuthenticationForUnauthenticatedUsers() throws Exception {
        mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return the authenticated user's notifications with mapped DTO fields")
    void shouldReturnUserNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + testBreederToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.content[?(@.message == 'Nowa nieprzeczytana wiadomość')].link").value("/link-1"))
                .andExpect(jsonPath("$.content[?(@.message == 'Nowa nieprzeczytana wiadomość')].type").value("ACCOUNT_APPROVED"))
                .andExpect(jsonPath("$.content[?(@.message == 'Nowa nieprzeczytana wiadomość')].isRead").value(false));
    }

    @Test
    @DisplayName("Should return older notifications on the next page")
    void shouldReturnOlderNotificationsOnNextPage()
            throws Exception {

        for (int i = 0; i < 60; i++) {
            Notification notification = new Notification();

            notification.setRecipient(testBreeder);
            notification.setMessage("Powiadomienie " + i);
            notification.setType(NotificationType.NEW_ANNOUNCEMENT);
            notification.setRead(false);

            notificationRepository.save(notification);
        }

        notificationRepository.flush();

        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + testBreederToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(50))
                .andExpect(jsonPath("$.totalElements").value(62))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.content[0].message").value("Powiadomienie 59"))
                .andExpect(jsonPath("$.content[49].message").value("Powiadomienie 10"));

        mockMvc.perform(get("/api/notifications")
                        .param("page", "1")
                        .param("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + testBreederToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(12))
                .andExpect(jsonPath("$.totalElements").value(62))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content[0].message").value("Powiadomienie 9"));
    }

    @Test
    @DisplayName("Should return at most 50 newest notifications")
    void shouldReturnAtMostFiftyNewestNotifications() throws Exception {
        for (int i = 0; i < 60; i++) {
            Notification notification = new Notification();
            notification.setRecipient(testBreeder);
            notification.setMessage("Powiadomienie limit " + i);
            notification.setLink("/limit/" + i);
            notification.setType(NotificationType.NEW_ANNOUNCEMENT);
            notification.setRead(false);

            notificationRepository.save(notification);
        }

        notificationRepository.flush();

        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + testBreederToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(50))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.content[0].message").value("Powiadomienie limit 59"))
                .andExpect(jsonPath("$.content[49].message").value("Powiadomienie limit 10"));
    }

    @Test
    @DisplayName("Should return the unread notification count for the authenticated user")
    void shouldReturnUnreadCount() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + testBreederToken))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("Should count all unread notifications even when the list is limited")
    void shouldCountAllUnreadNotificationsDespiteListLimit()
            throws Exception {

        for (int i = 0; i < 60; i++) {
            Notification notification = new Notification();
            notification.setRecipient(testBreeder);
            notification.setMessage("Nieprzeczytane " + i);
            notification.setType(NotificationType.NEW_ANNOUNCEMENT);
            notification.setRead(false);

            notificationRepository.save(notification);
        }

        notificationRepository.flush();

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + testBreederToken))
                .andExpect(status().isOk())
                .andExpect(content().string("61"));
    }

    @Test
    @DisplayName("Should mark a notification as read and decrease the unread count")
    void shouldMarkNotificationAsReadAndDecreaseCount() throws Exception {
        Long notificationId = unreadNotification.getId();

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + testBreederToken))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        mockMvc.perform(put("/api/notifications/" + notificationId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + testBreederToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + testBreederToken))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));

        Notification updatedNotification = notificationRepository.findById(notificationId).orElseThrow();
        assertTrue(updatedNotification.isRead());
    }

    @Test
    @DisplayName("Should reject attempts to mark another user's notification as read")
    void shouldPreventIdorWhenMarkingOthersNotificationAsRead() throws Exception {
        Long othersNotificationId = unreadNotification.getId();

        mockMvc.perform(put("/api/notifications/" + othersNotificationId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + anotherBreederToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 404 when marking a non-existent notification as read")
    void shouldReturn404WhenNotificationNotFound() throws Exception {
        Long nonExistentId = 999999L;

        mockMvc.perform(put("/api/notifications/" + nonExistentId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + testBreederToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should mark only the current user's notifications as read")
    void shouldMarkAllNotificationsAsRead() throws Exception {
        Notification extraUnread = new Notification();
        extraUnread.setRecipient(testBreeder);
        extraUnread.setMessage("Kolejne nieprzeczytane");
        extraUnread.setType(NotificationType.NEW_ANNOUNCEMENT);
        extraUnread.setRead(false);
        extraUnread = notificationRepository.save(extraUnread);

        Notification anotherBreederNotification = new Notification();
        anotherBreederNotification.setRecipient(anotherBreeder);
        anotherBreederNotification.setMessage("Powiadomienie innego hodowcy");
        anotherBreederNotification.setType(NotificationType.NEW_ANNOUNCEMENT);
        anotherBreederNotification.setRead(false);
        anotherBreederNotification = notificationRepository.save(anotherBreederNotification);

        mockMvc.perform(put("/api/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + testBreederToken))
                .andExpect(status().isOk());

        assertTrue(notificationRepository.findById(unreadNotification.getId()).orElseThrow().isRead());
        assertTrue(notificationRepository.findById(extraUnread.getId()).orElseThrow().isRead());
        assertTrue(notificationRepository.findById(readNotification.getId()).orElseThrow().isRead());

        Notification untouchedNotification = notificationRepository.findById(anotherBreederNotification.getId()).orElseThrow();
        assertFalse(untouchedNotification.isRead());
    }
}