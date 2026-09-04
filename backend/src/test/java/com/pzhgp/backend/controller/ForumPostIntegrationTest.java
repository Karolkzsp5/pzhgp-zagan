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

    private ObjectMapper objectMapper = new ObjectMapper();

    private ForumCategory category;
    private ForumThread thread;
    private ForumPost post;

    private Breeder threadAuthor;

    private String threadAuthorToken;
    private String postAuthorToken;
    private String otherBreederToken;
    private String modToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        Section section = new Section();
        section.setName("Sekcja Testowa");
        section.setSortOrder(1);
        sectionRepository.save(section);

        Breeder admin = new Breeder();
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

        threadAuthor = new Breeder();
        threadAuthor.setEmail("thread.author@test.pl");
        threadAuthor.setName("Jan");
        threadAuthor.setSurname("Autor Tematu");
        threadAuthor.setPhoneNumber("333333333");
        threadAuthor.setPasswordHash("hashed3");
        threadAuthor.setRole(Role.BREEDER);
        threadAuthor.setStatus(AccountStatus.ACTIVE);
        threadAuthor.setSection(section);
        breederRepository.save(threadAuthor);
        threadAuthorToken = jwtService.generateToken(threadAuthor);

        Breeder postAuthor = new Breeder();
        postAuthor.setEmail("post.author@test.pl");
        postAuthor.setName("Piotr");
        postAuthor.setSurname("Autor Posta");
        postAuthor.setPhoneNumber("444444444");
        postAuthor.setPasswordHash("hashed4");
        postAuthor.setRole(Role.BREEDER);
        postAuthor.setStatus(AccountStatus.ACTIVE);
        postAuthor.setSection(section);
        breederRepository.save(postAuthor);
        postAuthorToken = jwtService.generateToken(postAuthor);

        Breeder otherBreeder = new Breeder();
        otherBreeder.setEmail("other@test.pl");
        otherBreeder.setName("Kamil");
        otherBreeder.setSurname("Inny");
        otherBreeder.setPhoneNumber("555555555");
        otherBreeder.setPasswordHash("hashed5");
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
        thread.setAuthor(threadAuthor);
        thread.setTitle("Temat do dyskusji");
        thread.setIsLocked(false);
        thread.setIsPinned(false);
        thread.setViews(0);
        threadRepository.save(thread);

        ForumPost firstPost = new ForumPost();
        firstPost.setThread(thread);
        firstPost.setAuthor(threadAuthor);
        firstPost.setBody("Treść otwierająca wątek");
        postRepository.save(firstPost);

        post = new ForumPost();
        post.setThread(thread);
        post.setAuthor(postAuthor);
        post.setBody("Początkowa treść posta");
        postRepository.save(post);
    }


    @Test
    @DisplayName("GET /threads/{id}/posts - Should fetch mapped posts from H2")
    void getPostsByThread_ShouldReturnRealData() throws Exception {
        mockMvc.perform(get("/api/forum/threads/" + thread.getId() + "/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + postAuthorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[1].body").value("Początkowa treść posta"))
                .andExpect(jsonPath("$.content[1].authorName").value("Piotr Autor Posta"))
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
    @DisplayName("PUT /posts/{id} - As Author, should update post content in DB")
    void updatePost_AsAuthor_ShouldUpdateDb() throws Exception {
        ForumPostRequest request = new ForumPostRequest("Treść po modyfikacji");

        mockMvc.perform(put("/api/forum/posts/" + post.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + postAuthorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertEquals("Treść po modyfikacji", postRepository.findById(post.getId()).get().getBody());
    }

    @Test
    @DisplayName("DELETE /posts/{id} - As Author, should remove their own post from DB")
    void deletePost_AsAuthor_ShouldRemoveFromDb() throws Exception {
        mockMvc.perform(delete("/api/forum/posts/" + post.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + postAuthorToken))
                .andExpect(status().isNoContent());

        assertTrue(postRepository.findById(post.getId()).isEmpty());
    }

    @Test
    @DisplayName("DELETE /posts/{id} - Trying to delete the only post in thread should return 403")
    void deletePost_WhenOnlyOnePostInThread_ShouldReturn403() throws Exception {
        ForumThread singleThread = new ForumThread();
        singleThread.setCategory(category);
        singleThread.setAuthor(threadAuthor);
        singleThread.setTitle("Tylko jeden post");
        threadRepository.save(singleThread);

        ForumPost singlePost = new ForumPost();
        singlePost.setThread(singleThread);
        singlePost.setAuthor(threadAuthor);
        singlePost.setBody("To jest jedyny post");
        postRepository.save(singlePost);

        mockMvc.perform(delete("/api/forum/posts/" + singlePost.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + threadAuthorToken))
                .andExpect(status().isForbidden());

        assertTrue(postRepository.existsById(singlePost.getId()));
    }

    @Test
    @DisplayName("DELETE /posts/{id} - As Admin, should remove from DB")
    void deletePost_AsAdmin_ShouldRemoveFromDb() throws Exception {
        mockMvc.perform(delete("/api/forum/posts/" + post.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertTrue(postRepository.findById(post.getId()).isEmpty());
    }

    @Test
    @DisplayName("PUT /posts/{id} - As Moderator, trying to edit someone's post should return 403 Forbidden")
    void updatePost_AsModerator_ShouldReturn403() throws Exception {
        ForumPostRequest request = new ForumPostRequest("Moderator hakuje wpis");

        mockMvc.perform(put("/api/forum/posts/" + post.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertEquals("Początkowa treść posta", postRepository.findById(post.getId()).get().getBody());
    }

    @Test
    @DisplayName("DELETE /posts/{id} - As regular user, trying to delete someone's post should return 403 Forbidden")
    void deletePost_AsRegularUser_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/forum/posts/" + post.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken))
                .andExpect(status().isForbidden());

        assertTrue(postRepository.existsById(post.getId()));
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

        assertEquals(2, postRepository.count());
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