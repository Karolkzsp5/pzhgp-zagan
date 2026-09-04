package com.pzhgp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pzhgp.backend.dto.ForumThreadRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.*;
import com.pzhgp.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ForumThreadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private ForumCategoryRepository categoryRepository;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private ForumPostRepository postRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private ForumCategory category;
    private ForumThread thread;
    private Breeder admin;

    private String authorToken;
    private String otherBreederToken;
    private String modToken;
    private String adminToken;

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

        Breeder moderator = new Breeder();
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

        Breeder author = new Breeder();
        author.setEmail("author@test.pl");
        author.setName("Jan");
        author.setSurname("Autor");
        author.setPhoneNumber("333333333");
        author.setPasswordHash("hashed3");
        author.setRole(Role.BREEDER);
        author.setStatus(AccountStatus.ACTIVE);
        author.setSection(section);
        breederRepository.save(author);
        authorToken = jwtService.generateToken(author);

        Breeder otherBreeder = new Breeder();
        otherBreeder.setEmail("other@test.pl");
        otherBreeder.setName("Piotr");
        otherBreeder.setSurname("Inny");
        otherBreeder.setPhoneNumber("444444444");
        otherBreeder.setPasswordHash("hashed4");
        otherBreeder.setRole(Role.BREEDER);
        otherBreeder.setStatus(AccountStatus.ACTIVE);
        otherBreeder.setSection(section);
        breederRepository.save(otherBreeder);
        otherBreederToken = jwtService.generateToken(otherBreeder);

        category = new ForumCategory();
        category.setName("Kategoria Testowa");
        category.setAuthor(admin);
        categoryRepository.save(category);

        thread = new ForumThread();
        thread.setCategory(category);
        thread.setAuthor(author);
        thread.setTitle("Tytuł początkowy");
        thread.setIsLocked(false);
        thread.setIsPinned(false);
        thread.setViews(0);
        threadRepository.save(thread);

        ForumPost post = new ForumPost();
        post.setThread(thread);
        post.setAuthor(author);
        post.setBody("Treść wpisu testowego");
        postRepository.save(post);
    }


    @Test
    @DisplayName("GET /categories/{id}/threads - Should fetch real data from H2")
    void getThreadsByCategory_ShouldReturnRealData() throws Exception {
        mockMvc.perform(get("/api/forum/categories/" + category.getId() + "/threads")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Tytuł początkowy"))
                .andExpect(jsonPath("$.content[0].authorName").value("Jan Autor"))
                .andExpect(jsonPath("$.content[0].canDelete").value(true));
    }

    @Test
    @DisplayName("GET /threads/{id} - Should increment views in real DB")
    void getThread_ShouldIncrementViewsInDb() throws Exception {
        mockMvc.perform(get("/api/forum/threads/" + thread.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken))
                .andExpect(status().isOk());

        assertEquals(1, threadRepository.findById(thread.getId()).get().getViews());
    }

    @Test
    @DisplayName("POST /threads - Should save new thread, post to H2 DB and send NEW_THREAD notification")
    void createThread_ShouldSaveToDbAndNotify() throws Exception {
        long initialThreadCount = threadRepository.count();
        long initialNotificationCount = notificationRepository.count();

        ForumThreadRequest request = new ForumThreadRequest(category.getId(), "Nowy temat", "Nowa treść");

        mockMvc.perform(post("/api/forum/threads")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(initialThreadCount + 1, threadRepository.count());
        assertTrue(notificationRepository.count() > initialNotificationCount);

        List<Notification> allNotifications = notificationRepository.findAll();
        boolean hasNewThreadNotification = allNotifications.stream()
                .anyMatch(n -> n.getType() == NotificationType.NEW_THREAD);

        assertTrue(hasNewThreadNotification, "Baza powinna zawierać powiadomienie NEW_THREAD");
    }

    @Test
    @DisplayName("POST /threads - Unauthenticated user should be rejected (403 Forbidden)")
    void createThread_WhenUnauthenticated_ShouldFail() throws Exception {
        ForumThreadRequest request = new ForumThreadRequest(category.getId(), "Haker", "Próba ataku");

        mockMvc.perform(post("/api/forum/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertEquals(1, threadRepository.count());
    }

    @Test
    @DisplayName("DELETE /threads/{id} - As Author, should return 204 and remove from DB")
    void deleteThread_AsAuthor_ShouldRemoveFromDb() throws Exception {
        mockMvc.perform(delete("/api/forum/threads/" + thread.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isNoContent());

        assertTrue(threadRepository.findById(thread.getId()).isEmpty());
    }

    @Test
    @DisplayName("PUT /threads/{id}/lock - As Admin, should change status to true")
    void lockThread_AsAdmin_ShouldChangeDb() throws Exception {
        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/lock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertTrue(threadRepository.findById(thread.getId()).get().getIsLocked());
    }

    @Test
    @DisplayName("PUT /threads/{id}/lock - As Moderator, should return 200 OK")
    void lockThread_AsModerator_ShouldReturn200() throws Exception {
        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/lock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken))
                .andExpect(status().isOk());

        assertTrue(threadRepository.findById(thread.getId()).get().getIsLocked());
    }

    @Test
    @DisplayName("PUT /threads/{id}/lock - Role changed to BREEDER in DB after ADMIN JWT issuance should return 403")
    void lockThread_WhenAdminRoleDowngradedInDb_ShouldReturn403() throws Exception {
        admin.setRole(Role.BREEDER);
        breederRepository.save(admin);

        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/lock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        assertFalse(threadRepository.findById(thread.getId()).get().getIsLocked());
    }

    @Test
    @DisplayName("PUT /threads/{id}/pin - As Moderator, should return 200 OK")
    void pinThread_AsModerator_ShouldReturn200() throws Exception {
        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/pin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken))
                .andExpect(status().isOk());

        assertTrue(threadRepository.findById(thread.getId()).get().getIsPinned());
    }

    @Test
    @DisplayName("PUT /threads/{id}/pin - As Breeder, should return 403 Forbidden")
    void pinThread_AsBreeder_ShouldReturn403() throws Exception {
        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/pin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken))
                .andExpect(status().isForbidden());

        assertFalse(threadRepository.findById(thread.getId()).get().getIsPinned());
    }


    @Test
    @DisplayName("PUT /threads/{id}/lock - As Author (Breeder), should return 403 Forbidden because they are not Mod/Admin")
    void lockThread_AsBreederAuthor_ShouldReturn403() throws Exception {
        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/lock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isForbidden());

        assertFalse(threadRepository.findById(thread.getId()).get().getIsLocked());
    }

    @Test
    @DisplayName("DELETE /threads/{id} - As Other Breeder, should return 403 Forbidden")
    void deleteThread_AsOtherBreeder_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/forum/threads/" + thread.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken))
                .andExpect(status().isForbidden());

        assertTrue(threadRepository.existsById(thread.getId()));
    }


    @Test
    @DisplayName("GET /threads/9999 - Should return 404 Not Found for non-existent thread")
    void getThread_WhenThreadDoesNotExist_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/forum/threads/9999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /threads/9999 - Should return 404 Not Found for non-existent thread")
    void deleteThread_WhenThreadDoesNotExist_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/forum/threads/9999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /threads - As Moderator, should save new thread and return 201 Created")
    void createThread_AsModerator_ShouldReturn201() throws Exception {
        long initialThreadCount = threadRepository.count();
        ForumThreadRequest request = new ForumThreadRequest(category.getId(), "Temat Moderatora", "Treść stworzona przez moderatora");

        mockMvc.perform(post("/api/forum/threads")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(initialThreadCount + 1, threadRepository.count());
    }

    @Test
    @DisplayName("PUT /threads/{id}/pin - As Administrator, should change pin status to true and return 200 OK")
    void pinThread_AsAdmin_ShouldReturn200() throws Exception {
        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/pin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertTrue(threadRepository.findById(thread.getId()).get().getIsPinned());
    }
}