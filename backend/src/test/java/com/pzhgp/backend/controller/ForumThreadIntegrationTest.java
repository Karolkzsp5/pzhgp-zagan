package com.pzhgp.backend.controller;

import tools.jackson.databind.json.JsonMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.pzhgp.backend.support.CookieAuthTestSupport.cookieAuth;
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

    @Autowired
    private JsonMapper objectMapper;

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

        admin = createRealUser("admin@test.pl", "111111111", Role.ADMINISTRATOR, section);
        Breeder moderator = createRealUser("moderator@test.pl", "222222222", Role.MODERATOR, section);
        author = createRealUser("author@test.pl", "333333333", Role.BREEDER, section);
        Breeder otherBreeder = createRealUser("other@test.pl", "444444444", Role.BREEDER, section);

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

    private Breeder createRealUser(String email, String phoneNumber, Role role, Section section) {
        Breeder breeder = new Breeder();
        breeder.setEmail(email);
        breeder.setRole(role);
        breeder.setStatus(AccountStatus.ACTIVE);
        breeder.setName("Test");
        breeder.setSurname("User");
        breeder.setPhoneNumber(phoneNumber);
        breeder.setPasswordHash("hashed");
        breeder.setSection(section);
        return breederRepository.save(breeder);
    }

    @Test
    @DisplayName("Should return category threads with mapped permissions and reply counts")
    void getThreadsByCategory_ShouldReturnRealData() throws Exception {
        mockMvc.perform(get("/api/forum/categories/" + category.getId() + "/threads")
                        .with(cookieAuth(authorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Tytuł początkowy"))
                .andExpect(jsonPath("$.content[0].canDelete").value(true))
                .andExpect(jsonPath("$.content[0].repliesCount").value(1));
    }

    @Test
    @DisplayName("Should return 404 when listing threads in a non-existent category")
    void getThreadsByCategory_WhenCategoryNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/forum/categories/9999/threads")
                        .with(cookieAuth(authorToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should increase views for visitors but not for the thread author")
    void getThread_ShouldIncrementViewsInDb() throws Exception {
        Long threadId = thread.getId();

        mockMvc.perform(get("/api/forum/threads/" + threadId)
                        .with(cookieAuth(otherBreederToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.views").value(1));

        mockMvc.perform(get("/api/forum/threads/" + threadId)
                        .with(cookieAuth(otherBreederToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.views").value(2));

        mockMvc.perform(get("/api/forum/threads/" + threadId)
                        .with(cookieAuth(authorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.views").value(2));

        entityManager.flush();
        entityManager.clear();

        assertEquals(2, threadRepository.findById(threadId).orElseThrow().getViews());
    }

    @Test
    @DisplayName("Should refresh thread details without incrementing views when countView is false")
    void getThread_ShouldSkipViewIncrementWhenRequested() throws Exception {
        Long threadId = thread.getId();

        mockMvc.perform(get("/api/forum/threads/" + threadId)
                        .param("countView", "false")
                        .with(cookieAuth(otherBreederToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.views").value(0));

        mockMvc.perform(get("/api/forum/threads/" + threadId)
                        .with(cookieAuth(otherBreederToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.views").value(1));

        mockMvc.perform(get("/api/forum/threads/" + threadId)
                        .param("countView", "false")
                        .with(cookieAuth(otherBreederToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.views").value(1));

        entityManager.flush();
        entityManager.clear();
        assertEquals(1, threadRepository.findById(threadId).orElseThrow().getViews());
    }

    @Test
    @DisplayName("Should save a new thread and opening post and notify relevant users")
    void createThread_ShouldSaveToDbAndNotify() throws Exception {
        long initialThreadCount = threadRepository.count();
        long initialPostCount = postRepository.count();
        long initialNotificationCount = notificationRepository.count();

        ForumThreadRequest request = new ForumThreadRequest(category.getId(), "Nowy temat", "Zupełnie nowa treść startowa");

        mockMvc.perform(post("/api/forum/threads")
                        .with(cookieAuth(authorToken))
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

        boolean authorGotIt = allNotifications.stream().anyMatch(n -> n.getRecipient().getId().equals(author.getId()));
        assertFalse(authorGotIt, "Autor wątku nie powinien otrzymać powiadomienia o jego utworzeniu.");
    }

    @Test
    @DisplayName("Should return 404 when creating a thread in a non-existent category")
    void createThread_WhenCategoryIsInvalid_ShouldReturn404() throws Exception {
        ForumThreadRequest request = new ForumThreadRequest(9999L, "Wątek w pustkę", "Treść testowa");

        mockMvc.perform(post("/api/forum/threads")
                        .with(cookieAuth(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when creating a thread with a non-positive category ID")
    void createThread_WithNonPositiveCategoryId_ShouldReturn400() throws Exception {
        ForumThreadRequest request = new ForumThreadRequest(0L, "Poprawny tytuł", "Poprawna treść pierwszej wiadomości");

        mockMvc.perform(post("/api/forum/threads")
                        .with(cookieAuth(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("ID kategorii musi być liczbą dodatnią."));
    }

    @Test
    @DisplayName("Should return 400 when creating a thread with an empty title")
    void createThread_WithEmptyTitle_ShouldReturn400() throws Exception {
        ForumThreadRequest request = new ForumThreadRequest(category.getId(), "", "Prawidłowa treść");

        mockMvc.perform(post("/api/forum/threads")
                        .with(cookieAuth(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when creating a thread with an empty opening post")
    void createThread_WithEmptyContent_ShouldReturn400() throws Exception {
        ForumThreadRequest request = new ForumThreadRequest(category.getId(), "Prawidłowy tytuł", "");

        mockMvc.perform(post("/api/forum/threads")
                        .with(cookieAuth(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when the opening post exceeds the maximum length")
    void createThread_WithTooLongInitialPost_ShouldReturn400() throws Exception {
        ForumThreadRequest request = new ForumThreadRequest(category.getId(), "Poprawny tytuł", "a".repeat(20_001));

        mockMvc.perform(post("/api/forum/threads")
                        .with(cookieAuth(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Treść pierwszej wiadomości może mieć maksymalnie 20000 znaków."));
    }

    @Test
    @DisplayName("Should return 401 when an unauthenticated user creates a thread")
    void createThread_WhenUnauthenticated_ShouldFail() throws Exception {
        ForumThreadRequest request = new ForumThreadRequest(category.getId(), "Haker", "Próba ataku");

        mockMvc.perform(post("/api/forum/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow a thread author to update its title")
    void updateThreadTitle_AsAuthor_ShouldReturn200() throws Exception {
        Map<String, String> payload = Map.of("title", "Zaktualizowany piękny tytuł");

        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/title")
                        .with(cookieAuth(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        ForumThread updated = threadRepository.findById(thread.getId()).get();
        assertEquals("Zaktualizowany piękny tytuł", updated.getTitle());
    }

    @Test
    @DisplayName("Should return 400 when a new thread title is empty or too short")
    void updateThreadTitle_WithInvalidLengthBoundary_ShouldReturn400() throws Exception {
        Map<String, String> shortPayload = Map.of("title", "1234");
        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/title")
                        .with(cookieAuth(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPayload)))
                .andExpect(status().isBadRequest());

        String longTitle = "a".repeat(151);
        Map<String, String> longPayload = Map.of("title", longTitle);
        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/title")
                        .with(cookieAuth(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 403 when another breeder updates the thread title")
    void updateThreadTitle_AsOtherBreeder_ShouldReturn403() throws Exception {
        Map<String, String> payload = Map.of("title", "Włamuję się na twój wątek");

        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/title")
                        .with(cookieAuth(otherBreederToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());

        ForumThread unchanged = threadRepository.findById(thread.getId()).get();
        assertEquals("Tytuł początkowy", unchanged.getTitle());
    }

    @Test
    @DisplayName("Should return 404 when updating a non-existent thread")
    void updateThreadTitle_WhenMissing_ShouldReturn404() throws Exception {
        Map<String, String> payload = Map.of("title", "Gdzie ja jestem?");

        mockMvc.perform(put("/api/forum/threads/9999/title")
                        .with(cookieAuth(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should allow an administrator to lock and unlock a thread")
    void lockThread_AsAdmin_ShouldToggleProperly() throws Exception {
        assertFalse(threadRepository.findById(thread.getId()).get().getIsLocked());

        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/lock")
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isOk());

        assertTrue(threadRepository.findById(thread.getId()).get().getIsLocked());

        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/lock")
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isOk());

        assertFalse(threadRepository.findById(thread.getId()).get().getIsLocked());
    }

    @Test
    @DisplayName("Should allow a moderator to pin and unpin a thread")
    void pinThread_AsModerator_ShouldToggleProperly() throws Exception {
        assertFalse(threadRepository.findById(thread.getId()).get().getIsPinned());

        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/pin")
                        .with(cookieAuth(modToken)))
                .andExpect(status().isOk());

        assertTrue(threadRepository.findById(thread.getId()).get().getIsPinned());

        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/pin")
                        .with(cookieAuth(modToken)))
                .andExpect(status().isOk());

        assertFalse(threadRepository.findById(thread.getId()).get().getIsPinned());
    }

    @Test
    @DisplayName("Should return 403 when a breeder pins a thread")
    void pinThread_AsBreeder_ShouldReturn403() throws Exception {
        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/pin")
                        .with(cookieAuth(otherBreederToken)))
                .andExpect(status().isForbidden());

        assertFalse(threadRepository.findById(thread.getId()).get().getIsPinned());
    }

    @Test
    @DisplayName("Should return 403 when a breeder attempts to lock their own thread")
    void lockThread_AsBreederAuthor_ShouldReturn403() throws Exception {
        mockMvc.perform(put("/api/forum/threads/" + thread.getId() + "/lock")
                        .with(cookieAuth(authorToken)))
                .andExpect(status().isForbidden());

        assertFalse(threadRepository.findById(thread.getId()).get().getIsLocked());
    }

    @Test
    @DisplayName("Should allow the author to delete a thread and its posts")
    void deleteThread_AsAuthor_ShouldRemoveFromDb() throws Exception {
        mockMvc.perform(delete("/api/forum/threads/" + thread.getId())
                        .with(cookieAuth(authorToken)))
                .andExpect(status().isNoContent());

        assertTrue(threadRepository.findById(thread.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should return 403 when another breeder deletes a thread")
    void deleteThread_AsOtherBreeder_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/forum/threads/" + thread.getId())
                        .with(cookieAuth(otherBreederToken)))
                .andExpect(status().isForbidden());

        assertTrue(threadRepository.existsById(thread.getId()));
    }

    @Test
    @DisplayName("Should return 404 when deleting a non-existent thread")
    void deleteThread_WhenThreadDoesNotExist_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/forum/threads/9999")
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isNotFound());
    }
}