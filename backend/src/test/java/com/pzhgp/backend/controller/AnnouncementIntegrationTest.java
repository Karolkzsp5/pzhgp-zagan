package com.pzhgp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pzhgp.backend.dto.AnnouncementRequestDto;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.AnnouncementRepository;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.SectionRepository;
import com.pzhgp.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AnnouncementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private JwtService jwtService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private Breeder admin;
    private Breeder moderator;
    private Breeder standardBreeder;

    private String adminToken;
    private String modToken;
    private String breederToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM notifications");
        jdbcTemplate.execute("DELETE FROM announcements");
        jdbcTemplate.execute("DELETE FROM breeders");
        jdbcTemplate.execute("DELETE FROM sections");

        Section section = new Section();
        section.setName("Sekcja Testowa");
        section.setSortOrder(1);
        sectionRepository.save(section);

        admin = new Breeder();
        admin.setEmail("admin@test.pl");
        admin.setName("Admin");
        admin.setSurname("Testowy");
        admin.setPhoneNumber("111111111");
        admin.setPasswordHash("hashed1");
        admin.setRole(Role.ADMINISTRATOR);
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setSection(section);
        breederRepository.save(admin);
        adminToken = jwtService.generateToken(admin);

        moderator = new Breeder();
        moderator.setEmail("mod@test.pl");
        moderator.setName("Mod");
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
    @DisplayName("GET /api/announcements - Should fetch paginated data from real database (Public Access)")
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
                .andExpect(jsonPath("$.content[0].authorName").value("Admin Testowy"))
                .andExpect(jsonPath("$.content[0].canEdit").value(false))

                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("POST /api/announcements - Should save into real DB when requested by Administrator")
    void shouldCreateAnnouncementAndSaveToDatabase() throws Exception {
        AnnouncementRequestDto requestDto = new AnnouncementRequestDto("Tytuł Integracyjny", "Treść Integracyjna", false);
        assertEquals(0, announcementRepository.count());

        mockMvc.perform(post("/api/announcements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());

        List<Announcement> announcementsInDb = announcementRepository.findAll();
        assertEquals(1, announcementsInDb.size());
        assertEquals("Tytuł Integracyjny", announcementsInDb.getFirst().getTitle());
        assertEquals("admin@test.pl", announcementsInDb.getFirst().getAuthor().getEmail());
    }

    @Test
    @DisplayName("POST /api/announcements - Spring Security should block standard Breeder")
    void shouldBlockBreederFromCreatingAnnouncement() throws Exception {
        AnnouncementRequestDto requestDto = new AnnouncementRequestDto("Zły tytuł", "Nieważne", false);

        mockMvc.perform(post("/api/announcements")
                        .header("Authorization", "Bearer " + breederToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());

        assertEquals(0, announcementRepository.count());
    }

    @Test
    @DisplayName("DELETE /api/announcements/{id} - Administrator should successfully delete Moderator's post from real DB")
    void adminShouldDeleteModeratorsPostFromDatabase() throws Exception {
        Announcement announcement = new Announcement();
        announcement.setTitle("Ogłoszenie Moda");
        announcement.setContent("Treść Moda");
        announcement.setAuthor(moderator);
        announcementRepository.save(announcement);

        Long savedId = announcement.getId();
        assertEquals(1, announcementRepository.count());

        mockMvc.perform(delete("/api/announcements/{id}", savedId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertEquals(0, announcementRepository.count());
    }

    @Test
    @DisplayName("PUT /api/announcements/{id} - Should update real DB record when Author modifies it")
    void authorShouldUpdateOwnPostInDatabase() throws Exception {
        Announcement announcement = new Announcement();
        announcement.setTitle("Stary tytuł");
        announcement.setContent("Stara treść");
        announcement.setAuthor(moderator);
        announcementRepository.save(announcement);

        Long savedId = announcement.getId();
        AnnouncementRequestDto updateRequest = new AnnouncementRequestDto("Nowy tytuł", "Nowa treść", true);

        mockMvc.perform(put("/api/announcements/{id}", savedId)
                        .header("Authorization", "Bearer " + modToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        Announcement updatedInDb = announcementRepository.findById(savedId).orElseThrow();
        assertEquals("Nowy tytuł", updatedInDb.getTitle());
        assertEquals("Nowa treść", updatedInDb.getContent());
        assertTrue(updatedInDb.isPinned());
    }
}