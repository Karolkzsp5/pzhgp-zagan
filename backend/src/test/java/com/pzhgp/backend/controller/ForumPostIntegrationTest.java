package com.pzhgp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        Breeder admin = createRealUser("admin@test.pl", Role.ADMINISTRATOR, section);
        Breeder moderator = createRealUser("moderator@test.pl", Role.MODERATOR, section);
        threadAuthor = createRealUser("thread.author@test.pl", Role.BREEDER, section);
        Breeder postAuthor = createRealUser("post.author@test.pl", Role.BREEDER, section);
        Breeder otherBreeder = createRealUser("other@test.pl", Role.BREEDER, section);

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
    @DisplayName("GET /threads/{id}/posts - Should fetch mapped posts from H2")
    void getPostsByThread_ShouldReturnRealData() throws Exception {
        mockMvc.perform(get("/api/forum/threads/" + thread.getId() + "/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + postAuthorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[1].body").value("Początkowa treść posta hodowcy"))
                .andExpect(jsonPath("$.content[1].canEdit").value(true))
                .andExpect(jsonPath("$.content[1].canDelete").value(true));
    }

    @Test
    @DisplayName("POST /threads/{id}/posts - Should save post, update lastPostAt, and create NEW_REPLY notification")
    void addPost_ShouldSaveToDbAndUpdateThreadAndNotify() throws Exception {
        long initialPostCount = postRepository.count();
        long initialNotificationCount = notificationRepository.count();
        LocalDateTime beforePost = threadRepository.findById(thread.getId()).get().getLastPostAt();

        ForumPostRequest request = new ForumPostRequest("Zupełnie nowa odpowiedź");

        mockMvc.perform(post("/api/forum/threads/" + thread.getId() + "/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken)
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
    @DisplayName("POST /threads/{id}/posts - Should return 400 Bad Request when body is empty")
    void addPost_WithEmptyBody_ShouldReturn400() throws Exception {
        ForumPostRequest request = new ForumPostRequest("");

        mockMvc.perform(post("/api/forum/threads/" + thread.getId() + "/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + postAuthorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /threads/{id}/posts - As Thread Author, should save post but NOT create NEW_REPLY notification")
    void addPost_WhenReplierIsThreadAuthor_ShouldNotNotify() throws Exception {
        long initialPostCount = postRepository.count();
        long initialNotificationCount = notificationRepository.count();

        ForumPostRequest request = new ForumPostRequest("Podbijam swój własny temat");

        mockMvc.perform(post("/api/forum/threads/" + thread.getId() + "/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + threadAuthorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(initialPostCount + 1, postRepository.count());
        assertEquals(initialNotificationCount, notificationRepository.count());
    }

    @Test
    @DisplayName("PUT /posts/{id} - As Author, should update post content and assign editedAt timestamp in DB")
    void updatePost_AsAuthor_ShouldUpdateDbAndSetEditedAt() throws Exception {
        ForumPostRequest request = new ForumPostRequest("Treść po modyfikacji");

        assertNull(postRepository.findById(breederPost.getId()).get().getEditedAt());

        mockMvc.perform(put("/api/forum/posts/" + breederPost.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + postAuthorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ForumPost updated = postRepository.findById(breederPost.getId()).get();
        assertEquals("Treść po modyfikacji", updated.getBody());
        assertNotNull(updated.getEditedAt());
    }

    @Test
    @DisplayName("PUT /posts/{id} - Should return 400 Bad Request when body is empty")
    void updatePost_WithEmptyBody_ShouldReturn400() throws Exception {
        ForumPostRequest request = new ForumPostRequest("");

        mockMvc.perform(put("/api/forum/posts/" + breederPost.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + postAuthorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /posts/{id} - As Author, should remove their own post from DB")
    void deletePost_AsAuthor_ShouldRemoveFromDb() throws Exception {
        mockMvc.perform(delete("/api/forum/posts/" + breederPost.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + postAuthorToken))
                .andExpect(status().isNoContent());

        assertTrue(postRepository.findById(breederPost.getId()).isEmpty());
    }

    @Test
    @DisplayName("DELETE /posts/{id} - Trying to delete the only post in thread should return 403 Forbidden")
    void deletePost_WhenOnlyOnePostInThread_ShouldReturn403() throws Exception {
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
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + threadAuthorToken))
                .andExpect(status().isForbidden());

        assertTrue(postRepository.existsById(singlePost.getId()));
    }

    @Test
    @DisplayName("DELETE /posts/{id} - As Admin, should remove from DB")
    void deletePost_AsAdmin_ShouldRemoveFromDb() throws Exception {
        mockMvc.perform(delete("/api/forum/posts/" + breederPost.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertTrue(postRepository.findById(breederPost.getId()).isEmpty());
    }

    @Test
    @DisplayName("DELETE /posts/{id} - Moderator should successfully delete Breeder's post from DB")
    void deletePost_AsModeratorOnBreederPost_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/forum/posts/" + breederPost.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken))
                .andExpect(status().isNoContent());

        assertTrue(postRepository.findById(breederPost.getId()).isEmpty());
    }

    @Test
    @DisplayName("DELETE /posts/{id} - Moderator attempting to delete Admin's post should get 403 Forbidden")
    void deletePost_AsModeratorOnAdminPost_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/forum/posts/" + adminPost.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken))
                .andExpect(status().isForbidden());

        assertTrue(postRepository.existsById(adminPost.getId()));
    }

    @Test
    @DisplayName("PUT /posts/{id} - Regular breeder attempting to edit someone else's post gets 403 Forbidden")
    void updatePost_AsOtherBreeder_ShouldReturn403() throws Exception {
        ForumPostRequest request = new ForumPostRequest("Zmieniam sobie cudzy post");

        mockMvc.perform(put("/api/forum/posts/" + breederPost.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertEquals("Początkowa treść posta hodowcy", postRepository.findById(breederPost.getId()).get().getBody());
    }

    @Test
    @DisplayName("PUT /posts/{id} - As Moderator, trying to edit someone's post should return 403 Forbidden")
    void updatePost_AsModerator_ShouldReturn403() throws Exception {
        ForumPostRequest request = new ForumPostRequest("Moderator hakuje wpis");

        mockMvc.perform(put("/api/forum/posts/" + breederPost.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertEquals("Początkowa treść posta hodowcy", postRepository.findById(breederPost.getId()).get().getBody());
    }

    @Test
    @DisplayName("DELETE /posts/{id} - As regular user, trying to delete someone's post should return 403 Forbidden")
    void deletePost_AsRegularUser_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/forum/posts/" + breederPost.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken))
                .andExpect(status().isForbidden());

        assertTrue(postRepository.existsById(breederPost.getId()));
    }

    @Test
    @DisplayName("POST /threads/{id}/posts - Adding post to a locked thread should return 403 Forbidden")
    void addPost_WhenThreadIsLocked_ShouldReturn403() throws Exception {
        thread.setIsLocked(true);
        threadRepository.save(thread);

        ForumPostRequest request = new ForumPostRequest("Próba odpisu");

        mockMvc.perform(post("/api/forum/threads/" + thread.getId() + "/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /threads/{id}/posts - Unauthenticated user should be rejected (403 Forbidden)")
    void addPost_WhenUnauthenticated_ShouldFail() throws Exception {
        ForumPostRequest request = new ForumPostRequest("Anonim pyta");

        mockMvc.perform(post("/api/forum/threads/" + thread.getId() + "/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /threads/9999/posts - Should return 404 Not Found")
    void getPostsByThread_WhenThreadNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/forum/threads/9999/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /posts/9999 - Should return 404 Not Found")
    void updatePost_WhenPostNotFound_ShouldReturn404() throws Exception {
        ForumPostRequest request = new ForumPostRequest("Zmiana");

        mockMvc.perform(put("/api/forum/posts/9999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + postAuthorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}