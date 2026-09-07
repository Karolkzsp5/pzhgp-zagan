package com.pzhgp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pzhgp.backend.dto.ForumThreadRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.*;
import com.pzhgp.backend.service.JwtService;
import jakarta.persistence.EntityManager;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Forum Thread Integration Tests")
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

    @Autowired
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ForumCategory category;
    private ForumThread thread;
    private Breeder admin;
    private Breeder author;

    private String authorToken;
    private String otherBreederToken;
    private String modToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        Section section = new Section(null, "Sekcja Testowa", 1);
        sectionRepository.save(section);

        admin = createRealUser("admin@test.pl", Role.ADMINISTRATOR, section);
        Breeder moderator = createRealUser("moderator@test.pl", Role.MODERATOR, section);
        author = createRealUser("author@test.pl", Role.BREEDER, section);
        Breeder otherBreeder = createRealUser("other@test.pl", Role.BREEDER, section);

        adminToken = jwtService.generateToken(admin);
        modToken = jwtService.generateToken(moderator);
        authorToken = jwtService.generateToken(author);
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

        ForumPost post1 = new ForumPost();
        post1.setThread(thread);
        post1.setAuthor(author);
        post1.setBody("Treść wpisu testowego");
        postRepository.save(post1);

        ForumPost post2 = new ForumPost();
        post2.setThread(thread);
        post2.setAuthor(otherBreeder);
        post2.setBody("Odpowiedź na wpis");
        postRepository.save(post2);

        entityManager.flush();
        entityManager.clear();
    }

    private Breeder createRealUser(String email, Role role, Section section) {
        Breeder breeder = new Breeder();
        breeder.setEmail(email);
        breeder.setRole(role);
        breeder.setStatus(AccountStatus.ACTIVE);
        breeder.setName("Test");
        breeder.setSurname("User");
        breeder.setPhoneNumber(String.valueOf(System.nanoTime()).substring(0, 9));
        breeder.setPasswordHash("hashed");
        breeder.setSection(section);
        return breederRepository.save(breeder);
    }

    @Test
    @DisplayName("GET /categories/{id}/threads - Should fetch real data from H2")
    void getThreadsByCategory_ShouldReturnRealData() throws Exception {
        mockMvc.perform(get("/api/forum/categories/" + category.getId() + "/threads")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Tytuł początkowy"))
                .andExpect(jsonPath("$.content[0].canDelete").value(true))
                .andExpect(jsonPath("$.content[0].repliesCount").value(1));
    }

    @Test
    @DisplayName("GET /categories/9999/threads - Should return 404 for missing category")
    void getThreadsByCategory_WhenCategoryNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/forum/categories/9999/threads")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isNotFound());
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
    @DisplayName("POST /threads - Should save new thread, INITIAL POST to H2 DB and send NEW_THREAD notification")
    void createThread_ShouldSaveToDbAndNotify() throws Exception {
        long initialThreadCount = threadRepository.count();
        long initialPostCount = postRepository.count();
        long initialNotificationCount = notificationRepository.count();

        ForumThreadRequest request = new ForumThreadRequest(category.getId(), "Nowy temat", "Zupełnie nowa treść startowa");

        mockMvc.perform(post("/api/forum/threads")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(initialThreadCount + 1, threadRepository.count());
        assertEquals(initialPostCount + 1, postRepository.count());

        ForumPost createdPost = postRepository.findAll().stream()
                .filter(p -> p.getBody().equals("Zupełnie nowa treść startowa"))
                .findFirst().orElseThrow(() -> new AssertionError("Brak pierwszego wpisu w bazie"));

        assertEquals("Nowy temat", createdPost.getThread().getTitle());

        assertTrue(notificationRepository.count() > initialNotificationCount);
        List<Notification> allNotifications = notificationRepository.findAll();

        Notification newThreadNotif = allNotifications.stream()
                .filter(n -> n.getType() == NotificationType.NEW_THREAD)
                .findFirst().orElseThrow();

        assertEquals("/forum/thread/" + createdPost.getThread().getId(), newThreadNotif.getLink());

        boolean authorGotIt = allNotifications.stream()
                .anyMatch(n -> n.getRecipient().getId().equals(author.getId()));
        assertFalse(authorGotIt, "Autor wątku nie powinien otrzymać powiadomienia o jego utworzeniu.");
    }

    @Test
    @DisplayName("POST /threads - Should return 404 when CategoryId is invalid")
    void createThread_WhenCategoryIsInvalid_ShouldReturn404() throws Exception {
        ForumThreadRequest request = new ForumThreadRequest(9999L, "Wątek w pustkę", "Treść testowa");

        mockMvc.perform(post("/api/forum/threads")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /threads - Should return 400 Bad Request when title is empty")
    void createThread_WithEmptyTitle_ShouldReturn400() throws Exception {
        ForumThreadRequest request = new ForumThreadRequest(category.getId(), "", "Prawidłowa treść");
        mockMvc.perform(post("/api/forum/threads")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /threads - Should return 400 Bad Request when initial post content is empty")
    void createThread_WithEmptyContent_ShouldReturn400() throws Exception {
        ForumThreadRequest request = new ForumThreadRequest(category.getId(), "Prawidłowy tytuł", "");
        mockMvc.perform(post("/api/forum/threads")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /threads - Unauthenticated user should be rejected (403 Forbidden)")
    void createThread_WhenUnauthenticated_ShouldFail() throws Exception {
        ForumThreadRequest request = new ForumThreadRequest(category.getId(), "Haker", "Próba ataku");

        mockMvc.perform(post("/api/forum/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /threads/{id}/title - Author should be able to update thread title in DB")
    void updateThreadTitle_AsAuthor_ShouldReturn200() throws Exception {
        Map<String, String> payload = Map.of("title", "Zaktualizowany piękny tytuł");

        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/title")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        ForumThread updated = threadRepository.findById(thread.getId()).get();
        assertEquals("Zaktualizowany piękny tytuł", updated.getTitle());
    }

    @Test
    @DisplayName("PUT /threads/{id}/title - Should return 400 Bad Request for titles that are too short (4 chars) or empty")
    void updateThreadTitle_WithInvalidLengthBoundary_ShouldReturn400() throws Exception {
        Map<String, String> shortPayload = Map.of("title", "1234");
        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/title")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPayload)))
                .andExpect(status().isBadRequest());

        String longTitle = "a".repeat(151);
        Map<String, String> longPayload = Map.of("title", longTitle);
        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/title")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /threads/{id}/title - Other breeder attempting to update title should get 403 Forbidden")
    void updateThreadTitle_AsOtherBreeder_ShouldReturn403() throws Exception {
        Map<String, String> payload = Map.of("title", "Włamuję się na twój wątek");

        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/title")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());

        ForumThread unchanged = threadRepository.findById(thread.getId()).get();
        assertEquals("Tytuł początkowy", unchanged.getTitle());
    }

    @Test
    @DisplayName("PUT /threads/9999/title - Updating non-existent thread should return 404")
    void updateThreadTitle_WhenMissing_ShouldReturn404() throws Exception {
        Map<String, String> payload = Map.of("title", "Gdzie ja jestem?");

        mockMvc.perform(put("/api/forum/threads/9999/title")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
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
    @DisplayName("DELETE /threads/{id} - As Other Breeder, should return 403 Forbidden")
    void deleteThread_AsOtherBreeder_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/forum/threads/" + thread.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken))
                .andExpect(status().isForbidden());

        assertTrue(threadRepository.existsById(thread.getId()));
    }

    @Test
    @DisplayName("DELETE /threads/9999 - Should return 404 Not Found for non-existent thread")
    void deleteThread_WhenThreadDoesNotExist_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/forum/threads/9999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /threads/{id}/lock - As Admin, should be able to double-toggle status")
    void lockThread_AsAdmin_ShouldToggleProperly() throws Exception {
        assertFalse(threadRepository.findById(thread.getId()).get().getIsLocked());

        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/lock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertTrue(threadRepository.findById(thread.getId()).get().getIsLocked());

        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/lock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertFalse(threadRepository.findById(thread.getId()).get().getIsLocked());
    }

    @Test
    @DisplayName("PUT /threads/{id}/pin - As Moderator, should be able to double-toggle status")
    void pinThread_AsModerator_ShouldToggleProperly() throws Exception {
        assertFalse(threadRepository.findById(thread.getId()).get().getIsPinned());

        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/pin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken))
                .andExpect(status().isOk());

        assertTrue(threadRepository.findById(thread.getId()).get().getIsPinned());

        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/pin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken))
                .andExpect(status().isOk());

        assertFalse(threadRepository.findById(thread.getId()).get().getIsPinned());
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
}