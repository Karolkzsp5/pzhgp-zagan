package com.pzhgp.backend.controller;

import tools.jackson.databind.json.JsonMapper;
import com.pzhgp.backend.dto.AnnouncementRequestDto;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.AnnouncementRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.pzhgp.backend.support.CookieAuthTestSupport.cookieAuth;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Announcement Integration Tests")
class AnnouncementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JsonMapper objectMapper;

    private Breeder admin;
    private Breeder moderator;
    private Breeder standardBreeder;

    private String adminToken;
    private String modToken;
    private String breederToken;

    @BeforeEach
    void setUp() {
        Section section = new Section();
        section.setName("Sekcja Testowa");
        section.setSortOrder(1);
        sectionRepository.save(section);

        admin = new Breeder();
        admin.setEmail("admin@test.pl");
        admin.setName("Administrator");
        admin.setSurname("Testowy");
        admin.setPhoneNumber("111111111");
        admin.setPasswordHash("hashed1");
        admin.setRole(Role.ADMINISTRATOR);
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setSection(section);
        breederRepository.save(admin);
        adminToken = jwtService.generateToken(admin);

        moderator = new Breeder();
        moderator.setEmail("moderator@test.pl");
        moderator.setName("Moderator");
        moderator.setSurname("Testowy");
        moderator.setPhoneNumber("222222222");
        moderator.setPasswordHash("hashed2");
        moderator.setRole(Role.MODERATOR);
        moderator.setStatus(AccountStatus.ACTIVE);
        moderator.setSection(section);
        breederRepository.save(moderator);
        modToken = jwtService.generateToken(moderator);

        standardBreeder = new Breeder();
        standardBreeder.setEmail("hodowca@test.pl");
        standardBreeder.setName("Jan");
        standardBreeder.setSurname("Hodowca");
        standardBreeder.setPhoneNumber("333333333");
        standardBreeder.setPasswordHash("hashed3");
        standardBreeder.setRole(Role.BREEDER);
        standardBreeder.setStatus(AccountStatus.ACTIVE);
        standardBreeder.setSection(section);
        breederRepository.save(standardBreeder);
        breederToken = jwtService.generateToken(standardBreeder);
    }

    @Test
    @DisplayName("Should return paginated announcements to unauthenticated users")
    void shouldFetchAnnouncementsFromDatabase() throws Exception {
        Announcement announcement = new Announcement();
        announcement.setTitle("Prawdziwe Ogłoszenie");
        announcement.setContent("Treść ogłoszenia w bazie");
        announcement.setAuthor(admin);
        announcement.setPinned(true);
        announcementRepository.save(announcement);

        mockMvc.perform(get("/api/announcements")
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Prawdziwe Ogłoszenie"))
                .andExpect(jsonPath("$.content[0].authorName").value("Administrator Testowy"))
                .andExpect(jsonPath("$.content[0].canEdit").value(false))

                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("Should paginate announcements across multiple pages")
    void shouldPaginateAnnouncementsCorrectly() throws Exception {
        for (int i = 1; i <= 12; i++) {
            Announcement announcement = new Announcement();
            announcement.setTitle("Ogłoszenie nr " + i);
            announcement.setContent("Testowa treść " + i);
            announcement.setAuthor(admin);
            announcement.setPinned(false);
            announcementRepository.save(announcement);
        }

        assertEquals(12, announcementRepository.count());

        mockMvc.perform(get("/api/announcements")
                        .param("page", "1")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(5))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/announcements")
                        .param("page", "2")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(2))
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("Should disable editing but retain deletion permission after an announcement author's role is downgraded")
    void shouldReturnCorrectPermissionsAfterAuthorRoleChange() throws Exception {
        Announcement announcement = new Announcement();
        announcement.setTitle("Ogłoszenie Moderatora");
        announcement.setContent("Treść");
        announcement.setAuthor(moderator);
        announcement.setPinned(false);
        announcementRepository.save(announcement);

        moderator.setRole(Role.BREEDER);
        breederRepository.save(moderator);

        mockMvc.perform(get("/api/announcements").with(cookieAuth(modToken))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].canEdit").value(false))
                .andExpect(jsonPath("$.content[0].canDelete").value(true));
    }

    @Test
    @DisplayName("Should save an announcement created by an administrator")
    void shouldCreateAnnouncementAndSaveToDatabase() throws Exception {
        AnnouncementRequestDto requestDto = new AnnouncementRequestDto("Tytuł Integracyjny", "Treść Integracyjna", false);
        assertEquals(0, announcementRepository.count());

        mockMvc.perform(post("/api/announcements")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());

        List<Announcement> announcementsInDb = announcementRepository.findAll();
        assertEquals(1, announcementsInDb.size());
        assertEquals("Tytuł Integracyjny", announcementsInDb.getFirst().getTitle());
        assertEquals("admin@test.pl", announcementsInDb.getFirst().getAuthor().getEmail());
    }

    @Test
    @DisplayName("Should allow a moderator to create an announcement")
    void shouldAllowModeratorToCreateAnnouncement() throws Exception {
        AnnouncementRequestDto requestDto = new AnnouncementRequestDto("Lot próbny", "Informacje dla hodowców", false);

        mockMvc.perform(post("/api/announcements")
                        .with(cookieAuth(modToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());

        assertEquals(1, announcementRepository.count());
    }

    @Test
    @DisplayName("Should notify other active breeders when an announcement is created")
    void shouldCreateNotificationsOnAnnouncementCreation() throws Exception {
        AnnouncementRequestDto requestDto = new AnnouncementRequestDto("Nowe ogłoszenie", "Ważne info", true);

        mockMvc.perform(post("/api/announcements")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());

        List<Notification> notifications = notificationRepository.findAll();

        assertEquals(2, notifications.size());

        assertTrue(notifications.stream().anyMatch(n -> n.getRecipient().getEmail().equals("moderator@test.pl")));
        assertTrue(notifications.stream().anyMatch(n -> n.getRecipient().getEmail().equals("hodowca@test.pl")));
        assertFalse(notifications.stream().anyMatch(n -> n.getRecipient().getEmail().equals("admin@test.pl")));
    }

    @Test
    @DisplayName("Should return 403 when a breeder creates an announcement")
    void shouldBlockBreederFromCreatingAnnouncement() throws Exception {
        AnnouncementRequestDto requestDto = new AnnouncementRequestDto("Zły tytuł", "Nieważne", false);

        mockMvc.perform(post("/api/announcements")
                        .with(cookieAuth(breederToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());

        assertEquals(0, announcementRepository.count());
    }

    @Test
    @DisplayName("Should return 400 when announcement content exceeds the maximum length")
    void shouldRejectAnnouncementWithTooLongContent() throws Exception {
        AnnouncementRequestDto requestDto = new AnnouncementRequestDto("Poprawny tytuł", "a".repeat(50_001), false);

        mockMvc.perform(post("/api/announcements").with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Treść ogłoszenia może mieć maksymalnie 50000 znaków."));

        assertEquals(0, announcementRepository.count());
    }

    @Test
    @DisplayName("Should deny announcement creation when the user's current database role no longer permits it")
    void shouldReflectDbRoleChangeDespiteOldJwt() throws Exception {
        Breeder currentAdmin = breederRepository.findByEmail("admin@test.pl").orElseThrow();
        currentAdmin.setRole(Role.BREEDER);
        breederRepository.save(currentAdmin);

        AnnouncementRequestDto requestDto = new AnnouncementRequestDto("Próba ataku", "Mam stary token!", false);

        mockMvc.perform(post("/api/announcements")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should save changes when an announcement author edits their announcement")
    void authorShouldUpdateOwnPostInDatabase() throws Exception {
        Announcement announcement = new Announcement();
        announcement.setTitle("Stary tytuł");
        announcement.setContent("Stara treść");
        announcement.setAuthor(moderator);
        announcementRepository.save(announcement);

        Long savedId = announcement.getId();
        AnnouncementRequestDto updateRequest = new AnnouncementRequestDto("Nowy tytuł", "Nowa treść", true);

        mockMvc.perform(put("/api/announcements/{id}", savedId)
                        .with(cookieAuth(modToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        Announcement updatedInDb = announcementRepository.findById(savedId).orElseThrow();
        assertEquals("Nowy tytuł", updatedInDb.getTitle());
        assertEquals("Nowa treść", updatedInDb.getContent());
        assertTrue(updatedInDb.isPinned());
    }

    @Test
    @DisplayName("Should return 403 when a breeder tries to update an announcement")
    void shouldDenyBreederFromUpdatingAnnouncement() throws Exception {
        Announcement announcement = new Announcement();
        announcement.setTitle("Ogłoszenie Admina");
        announcement.setContent("Treść");
        announcement.setAuthor(admin);
        announcementRepository.save(announcement);

        AnnouncementRequestDto requestDto = new AnnouncementRequestDto("Zmieniony tytuł", "Treść", false);

        mockMvc.perform(put("/api/announcements/{id}", announcement.getId())
                        .with(cookieAuth(breederToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow an administrator to delete a moderator's announcement")
    void adminShouldDeleteModeratorsPostFromDatabase() throws Exception {
        Announcement announcement = new Announcement();
        announcement.setTitle("Ogłoszenie Moderatora");
        announcement.setContent("Treść Moderatora");
        announcement.setAuthor(moderator);
        announcementRepository.save(announcement);

        Long savedId = announcement.getId();
        assertEquals(1, announcementRepository.count());

        mockMvc.perform(delete("/api/announcements/{id}", savedId).with(cookieAuth(adminToken))).andExpect(status().isNoContent());
        assertEquals(0, announcementRepository.count());
    }
}