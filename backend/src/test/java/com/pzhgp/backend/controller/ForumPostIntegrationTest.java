package com.pzhgp.backend.controller;

import tools.jackson.databind.json.JsonMapper;
import com.pzhgp.backend.dto.ForumPostRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.*;
import com.pzhgp.backend.service.JwtService;
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

import java.time.LocalDateTime;
import java.util.List;

import static com.pzhgp.backend.support.CookieAuthTestSupport.cookieAuth;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Forum Post Integration Tests")
class ForumPostIntegrationTest {

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
    private JsonMapper objectMapper;

    private ForumCategory category;
    private ForumThread thread;
    private ForumPost breederPost;
    private ForumPost adminPost;

    private Breeder threadAuthor;

    private String threadAuthorToken;
    private String postAuthorToken;
    private String otherBreederToken;
    private String modToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        Section section = new Section(null, "Sekcja Testowa", 1);
        sectionRepository.save(section);

        Breeder admin = createRealUser("admin@test.pl", "111111111", Role.ADMINISTRATOR, section);
        Breeder moderator = createRealUser("moderator@test.pl", "222222222", Role.MODERATOR, section);
        threadAuthor = createRealUser("thread.author@test.pl", "333333333", Role.BREEDER, section);
        Breeder postAuthor = createRealUser("post.author@test.pl", "444444444", Role.BREEDER, section);
        Breeder otherBreeder = createRealUser("other@test.pl", "555555555", Role.BREEDER, section);

        adminToken = jwtService.generateToken(admin);
        modToken = jwtService.generateToken(moderator);
        threadAuthorToken = jwtService.generateToken(threadAuthor);
        postAuthorToken = jwtService.generateToken(postAuthor);
        otherBreederToken = jwtService.generateToken(otherBreeder);

        category = new ForumCategory();
        category.setName("Kategoria Testowa");
        category.setAuthor(admin);
        categoryRepository.save(category);

        thread = new ForumThread();
        thread.setCategory(category);
        thread.setAuthor(threadAuthor);
        thread.setTitle("Temat do dyskusji");
        thread.setIsLocked(false);
        thread.setLastPostAt(LocalDateTime.now().minusDays(1));
        threadRepository.save(thread);

        adminPost = new ForumPost();
        adminPost.setThread(thread);
        adminPost.setAuthor(admin);
        adminPost.setBody("Admin otworzył wątek");
        postRepository.save(adminPost);

        breederPost = new ForumPost();
        breederPost.setThread(thread);
        breederPost.setAuthor(postAuthor);
        breederPost.setBody("Początkowa treść posta hodowcy");
        postRepository.save(breederPost);
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
    @DisplayName("Should return mapped posts belonging to the requested thread")
    void getPostsByThread_ShouldReturnRealData() throws Exception {
        mockMvc.perform(get("/api/forum/threads/" + thread.getId() + "/posts")
                        .with(cookieAuth(postAuthorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[1].body").value("Początkowa treść posta hodowcy"))
                .andExpect(jsonPath("$.content[1].canEdit").value(true))
                .andExpect(jsonPath("$.content[1].canDelete").value(true));
    }

    @Test
    @DisplayName("Should return 404 when listing posts in a non-existent thread")
    void getPostsByThread_WhenThreadNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/forum/threads/9999/posts")
                        .with(cookieAuth(otherBreederToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should save a reply, update thread activity time and notify the thread author")
    void addPost_ShouldSaveToDbAndUpdateThreadAndNotify() throws Exception {
        long initialPostCount = postRepository.count();
        long initialNotificationCount = notificationRepository.count();
        LocalDateTime beforePost = threadRepository.findById(thread.getId()).get().getLastPostAt();

        ForumPostRequest request = new ForumPostRequest("Zupełnie nowa odpowiedź");

        mockMvc.perform(post("/api/forum/threads/" + thread.getId() + "/posts")
                        .with(cookieAuth(otherBreederToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(initialPostCount + 1, postRepository.count());

        ForumThread updatedThread = threadRepository.findById(thread.getId()).get();
        assertTrue(updatedThread.getLastPostAt().isAfter(beforePost));

        assertEquals(initialNotificationCount + 1, notificationRepository.count());

        List<Notification> allNotifications = notificationRepository.findAll();
        Notification replyNotification = allNotifications.stream()
                .filter(n -> n.getType() == NotificationType.NEW_REPLY)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Brak powiadomienia NEW_REPLY w bazie!"));

        assertEquals(thread.getAuthor().getId(), replyNotification.getRecipient().getId());
        assertEquals("/forum/thread/" + thread.getId(), replyNotification.getLink());
        assertTrue(replyNotification.getMessage().contains("dodał/a odpowiedź w twoim wątku na forum"));
    }

    @Test
    @DisplayName("Should save a reply without notifying the thread author about their own post")
    void addPost_WhenReplierIsThreadAuthor_ShouldNotNotify() throws Exception {
        long initialPostCount = postRepository.count();
        long initialNotificationCount = notificationRepository.count();

        ForumPostRequest request = new ForumPostRequest("Podbijam swój własny temat");

        mockMvc.perform(post("/api/forum/threads/" + thread.getId() + "/posts")
                        .with(cookieAuth(threadAuthorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(initialPostCount + 1, postRepository.count());
        assertEquals(initialNotificationCount, notificationRepository.count());
    }

    @Test
    @DisplayName("Should return 400 when creating a post with empty content")
    void addPost_WithEmptyBody_ShouldReturn400() throws Exception {
        ForumPostRequest request = new ForumPostRequest("");

        mockMvc.perform(post("/api/forum/threads/" + thread.getId() + "/posts")
                        .with(cookieAuth(postAuthorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when post content exceeds the maximum length")
    void addPost_WithTooLongBody_ShouldReturn400() throws Exception {
        ForumPostRequest request = new ForumPostRequest("a".repeat(20_001));

        mockMvc.perform(post("/api/forum/threads/" + thread.getId() + "/posts")
                        .with(cookieAuth(postAuthorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Treść wpisu może mieć maksymalnie 20000 znaków."));
    }

    @Test
    @DisplayName("Should return 409 when adding a post to a locked thread")
    void addPost_WhenThreadIsLocked_ShouldReturn409() throws Exception {
        thread.setIsLocked(true);
        threadRepository.save(thread);

        ForumPostRequest request = new ForumPostRequest("Próba odpisu");

        mockMvc.perform(post("/api/forum/threads/" + thread.getId() + "/posts")
                        .with(cookieAuth(otherBreederToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return 401 when an unauthenticated user adds a post")
    void addPost_WhenUnauthenticated_ShouldFail() throws Exception {
        ForumPostRequest request = new ForumPostRequest("Anonim pyta");

        mockMvc.perform(post("/api/forum/threads/" + thread.getId() + "/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow the author to update post content and set the edit timestamp")
    void updatePost_AsAuthor_ShouldUpdateDbAndSetEditedAt() throws Exception {
        ForumPostRequest request = new ForumPostRequest("Treść po modyfikacji");

        assertNull(postRepository.findById(breederPost.getId()).get().getEditedAt());

        mockMvc.perform(put("/api/forum/posts/" + breederPost.getId())
                        .with(cookieAuth(postAuthorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ForumPost updated = postRepository.findById(breederPost.getId()).get();
        assertEquals("Treść po modyfikacji", updated.getBody());
        assertNotNull(updated.getEditedAt());
    }

    @Test
    @DisplayName("Should return 400 when updating a post with empty content")
    void updatePost_WithEmptyBody_ShouldReturn400() throws Exception {
        ForumPostRequest request = new ForumPostRequest("");

        mockMvc.perform(put("/api/forum/posts/" + breederPost.getId())
                        .with(cookieAuth(postAuthorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 403 when a breeder edits another breeder's post")
    void updatePost_AsOtherBreeder_ShouldReturn403() throws Exception {
        ForumPostRequest request = new ForumPostRequest("Zmieniam sobie cudzy post");

        mockMvc.perform(put("/api/forum/posts/" + breederPost.getId())
                        .with(cookieAuth(otherBreederToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertEquals("Początkowa treść posta hodowcy", postRepository.findById(breederPost.getId()).get().getBody());
    }

    @Test
    @DisplayName("Should return 403 when a moderator edits another user's post")
    void updatePost_AsModerator_ShouldReturn403() throws Exception {
        ForumPostRequest request = new ForumPostRequest("Moderator hakuje wpis");

        mockMvc.perform(put("/api/forum/posts/" + breederPost.getId())
                        .with(cookieAuth(modToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertEquals("Początkowa treść posta hodowcy", postRepository.findById(breederPost.getId()).get().getBody());
    }

    @Test
    @DisplayName("Should return 404 when updating a non-existent post")
    void updatePost_WhenPostNotFound_ShouldReturn404() throws Exception {
        ForumPostRequest request = new ForumPostRequest("Zmiana");

        mockMvc.perform(put("/api/forum/posts/9999")
                        .with(cookieAuth(postAuthorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should allow an author to delete a reply and update thread activity time")
    void deletePost_AsAuthor_ShouldRemoveFromDb() throws Exception {
        LocalDateTime expectedLastPostAt = adminPost.getCreatedAt();

        mockMvc.perform(delete("/api/forum/posts/" + breederPost.getId())
                .with(cookieAuth(postAuthorToken))).andExpect(status().isNoContent());

        assertTrue(postRepository.findById(breederPost.getId()).isEmpty());
        ForumThread updatedThread = threadRepository.findById(thread.getId()).orElseThrow();
        assertEquals(expectedLastPostAt, updatedThread.getLastPostAt());
    }

    @Test
    @DisplayName("Should allow an administrator to delete another user's post")
    void deletePost_AsAdmin_ShouldRemoveFromDb() throws Exception {
        mockMvc.perform(delete("/api/forum/posts/" + breederPost.getId())
                        .with(cookieAuth(adminToken)))
                .andExpect(status().isNoContent());

        assertTrue(postRepository.findById(breederPost.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should allow a moderator to delete a breeder's post")
    void deletePost_AsModeratorOnBreederPost_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/forum/posts/" + breederPost.getId())
                        .with(cookieAuth(modToken)))
                .andExpect(status().isNoContent());

        assertTrue(postRepository.findById(breederPost.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should return 409 when deleting the only post in a thread")
    void deletePost_WhenOnlyOnePostInThread_ShouldReturn409() throws Exception {
        ForumThread singleThread = new ForumThread();
        singleThread.setCategory(category);
        singleThread.setAuthor(threadAuthor);
        singleThread.setTitle("Wątek z 1 postem");
        singleThread.setLastPostAt(LocalDateTime.now());
        threadRepository.save(singleThread);

        ForumPost singlePost = new ForumPost();
        singlePost.setThread(singleThread);
        singlePost.setAuthor(threadAuthor);
        singlePost.setBody("To jest jedyny post w tym wątku");
        postRepository.save(singlePost);

        mockMvc.perform(delete("/api/forum/posts/" + singlePost.getId())
                        .with(cookieAuth(threadAuthorToken)))
                .andExpect(status().isConflict());

        assertTrue(postRepository.existsById(singlePost.getId()));
    }

    @Test
    @DisplayName("Should return 409 when deleting an opening post from a thread with replies")
    void deletePost_WhenPostIsOpeningPost_ShouldReturn409() throws Exception {
        mockMvc.perform(delete("/api/forum/posts/" + adminPost.getId()).with(cookieAuth(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Nie można usunąć pierwszego wpisu wątku. Usuń cały wątek zamiast jego pierwszego wpisu."));

        assertTrue(postRepository.existsById(adminPost.getId()));
        assertTrue(postRepository.existsById(breederPost.getId()));
    }

    @Test
    @DisplayName("Should return 403 when a moderator deletes an administrator's post")
    void deletePost_AsModeratorOnAdminPost_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/forum/posts/" + adminPost.getId())
                        .with(cookieAuth(modToken)))
                .andExpect(status().isForbidden());

        assertTrue(postRepository.existsById(adminPost.getId()));
    }

    @Test
    @DisplayName("Should return 403 when a breeder deletes another user's post")
    void deletePost_AsRegularUser_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/forum/posts/" + breederPost.getId())
                .with(cookieAuth(otherBreederToken))).andExpect(status().isForbidden());
        assertTrue(postRepository.existsById(breederPost.getId()));
    }
}