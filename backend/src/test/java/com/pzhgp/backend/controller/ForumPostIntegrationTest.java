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
class ForumPostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ForumTopicRepository topicRepository;

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

    private ForumThread topic;
    private ForumPost post;

    private String topicAuthorToken;
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

        Breeder topicAuthor = new Breeder();
        topicAuthor.setEmail("topic.author@test.pl");
        topicAuthor.setName("Jan");
        topicAuthor.setSurname("Autor Tematu");
        topicAuthor.setPhoneNumber("333333333");
        topicAuthor.setPasswordHash("hashed3");
        topicAuthor.setRole(Role.BREEDER);
        topicAuthor.setStatus(AccountStatus.ACTIVE);
        topicAuthor.setSection(section);
        breederRepository.save(topicAuthor);
        topicAuthorToken = jwtService.generateToken(topicAuthor);

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

        ForumCategory category = new ForumCategory();
        category.setName("Kategoria Testowa");
        categoryRepository.save(category);

        topic = new ForumThread();
        topic.setCategory(category);
        topic.setAuthor(topicAuthor);
        topic.setTitle("Temat do dyskusji");
        topic.setIsLocked(false);
        topic.setIsPinned(false);
        topic.setViews(0);
        topicRepository.save(topic);

        post = new ForumPost();
        post.setTopic(topic);
        post.setAuthor(postAuthor);
        post.setBody("Początkowa treść posta");
        postRepository.save(post);
    }


    @Test
    @DisplayName("GET /topics/{id}/posts - Should fetch mapped posts from H2")
    void getPostsByTopic_ShouldReturnRealData() throws Exception {
        mockMvc.perform(get("/api/forum/topics/" + topic.getId() + "/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + postAuthorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].body").value("Początkowa treść posta"))
                .andExpect(jsonPath("$.content[0].authorName").value("Piotr Autor Posta"))
                .andExpect(jsonPath("$.content[0].canEdit").value(true))
                .andExpect(jsonPath("$.content[0].canDelete").value(true));
    }

    @Test
    @DisplayName("POST /topics/{id}/posts - Should save post, update lastPostAt, and create NEW_REPLY notification")
    void addPost_ShouldSaveToDbAndUpdateTopicAndNotify() throws Exception {
        long initialPostCount = postRepository.count();
        long initialNotificationCount = notificationRepository.count();
        LocalDateTime beforePost = topicRepository.findById(topic.getId()).get().getLastPostAt();

        ForumPostRequest request = new ForumPostRequest("Zupełnie nowa odpowiedź");

        mockMvc.perform(post("/api/forum/topics/" + topic.getId() + "/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(initialPostCount + 1, postRepository.count());

        ForumThread updatedTopic = topicRepository.findById(topic.getId()).get();
        assertTrue(updatedTopic.getLastPostAt().isAfter(beforePost));

        assertEquals(initialNotificationCount + 1, notificationRepository.count());

        List<Notification> allNotifications = notificationRepository.findAll();
        Notification replyNotification = allNotifications.stream()
                .filter(n -> n.getType() == NotificationType.NEW_REPLY)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Brak powiadomienia NEW_REPLY w bazie!"));

        assertEquals(topic.getAuthor().getId(), replyNotification.getRecipient().getId());
    }

    @Test
    @DisplayName("POST /topics/{id}/posts - As Topic Author, should save post but NOT create NEW_REPLY notification")
    void addPost_WhenReplierIsTopicAuthor_ShouldNotNotify() throws Exception {
        long initialPostCount = postRepository.count();
        long initialNotificationCount = notificationRepository.count();

        ForumPostRequest request = new ForumPostRequest("Podbijam swój własny temat");

        mockMvc.perform(post("/api/forum/topics/" + topic.getId() + "/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + topicAuthorToken)
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
    @DisplayName("POST /topics/{id}/posts - Adding post to a locked topic should return 403 Forbidden")
    void addPost_WhenTopicIsLocked_ShouldReturn403() throws Exception {
        topic.setIsLocked(true);
        topicRepository.save(topic);

        ForumPostRequest request = new ForumPostRequest("Próba odpisu");

        mockMvc.perform(post("/api/forum/topics/" + topic.getId() + "/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertEquals(1, postRepository.count());
    }

    @Test
    @DisplayName("POST /topics/{id}/posts - Unauthenticated user should be rejected (403 Forbidden)")
    void addPost_WhenUnauthenticated_ShouldFail() throws Exception {
        ForumPostRequest request = new ForumPostRequest("Anonim pyta");

        mockMvc.perform(post("/api/forum/topics/" + topic.getId() + "/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("GET /topics/9999/posts - Should return 404 Not Found")
    void getPostsByTopic_WhenTopicNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/forum/topics/9999/posts")
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