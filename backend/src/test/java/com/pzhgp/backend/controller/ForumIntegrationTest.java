package com.pzhgp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pzhgp.backend.dto.ForumTopicRequest;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ForumIntegrationTest {

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
    private JwtService jwtService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private ForumCategory category;
    private ForumTopic topic;

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
        categoryRepository.save(category);

        topic = new ForumTopic();
        topic.setCategory(category);
        topic.setAuthor(author);
        topic.setTitle("Tytuł początkowy");
        topic.setIsLocked(false);
        topic.setIsPinned(false);
        topic.setViews(0);
        topicRepository.save(topic);

        ForumPost post = new ForumPost();
        post.setTopic(topic);
        post.setAuthor(author);
        post.setBody("Treść wpisu testowego");
        postRepository.save(post);
    }


    @Test
    @DisplayName("GET /categories/{id}/topics - Should fetch real data from H2")
    void getTopicsByCategory_ShouldReturnRealData() throws Exception {
        mockMvc.perform(get("/api/forum/categories/" + category.getId() + "/topics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Tytuł początkowy"))
                .andExpect(jsonPath("$.content[0].authorName").value("Jan Autor"))
                .andExpect(jsonPath("$.content[0].canDelete").value(true));
    }

    @Test
    @DisplayName("GET /topics/{id} - Should increment views in real DB")
    void getTopic_ShouldIncrementViewsInDb() throws Exception {
        mockMvc.perform(get("/api/forum/topics/" + topic.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken))
                .andExpect(status().isOk());

        assertEquals(1, topicRepository.findById(topic.getId()).get().getViews());
    }

    @Test
    @DisplayName("POST /topics - Should save new topic and post to H2 DB")
    void createTopic_ShouldSaveToDb() throws Exception {
        long initialTopicCount = topicRepository.count();
        ForumTopicRequest request = new ForumTopicRequest(category.getId(), "Nowy temat", "Nowa treść");

        mockMvc.perform(post("/api/forum/topics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(initialTopicCount + 1, topicRepository.count());
    }

    @Test
    @DisplayName("POST /topics - Unauthenticated user should be rejected (403 Forbidden)")
    void createTopic_WhenUnauthenticated_ShouldFail() throws Exception {
        ForumTopicRequest request = new ForumTopicRequest(category.getId(), "Haker", "Próba ataku");

        mockMvc.perform(post("/api/forum/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertEquals(1, topicRepository.count());
    }

    @Test
    @DisplayName("DELETE /topics/{id} - As Author, should return 204 and remove from DB")
    void deleteTopic_AsAuthor_ShouldRemoveFromDb() throws Exception {
        mockMvc.perform(delete("/api/forum/topics/" + topic.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isNoContent());

        assertTrue(topicRepository.findById(topic.getId()).isEmpty());
    }

    @Test
    @DisplayName("PUT /topics/{id}/lock - As Admin, should change status to true")
    void lockTopic_AsAdmin_ShouldChangeDb() throws Exception {
        mockMvc.perform(put("/api/forum/topics/" + topic.getId() + "/lock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertTrue(topicRepository.findById(topic.getId()).get().getIsLocked());
    }

    @Test
    @DisplayName("PUT /topics/{id}/lock - As Moderator, should return 200 OK")
    void lockTopic_AsModerator_ShouldReturn200() throws Exception {
        mockMvc.perform(put("/api/forum/topics/" + topic.getId() + "/lock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken))
                .andExpect(status().isOk());

        assertTrue(topicRepository.findById(topic.getId()).get().getIsLocked());
    }

    @Test
    @DisplayName("PUT /topics/{id}/lock - Role changed to BREEDER in DB after ADMIN JWT issuance should return 403")
    void lockTopic_WhenAdminRoleDowngradedInDb_ShouldReturn403() throws Exception {
        Breeder adminUser = breederRepository.findByEmail("admin@test.pl").get();
        adminUser.setRole(Role.BREEDER);
        breederRepository.save(adminUser);

        mockMvc.perform(put("/api/forum/topics/" + topic.getId() + "/lock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        assertFalse(topicRepository.findById(topic.getId()).get().getIsLocked());
    }

    @Test
    @DisplayName("PUT /topics/{id}/pin - As Moderator, should return 200 OK")
    void pinTopic_AsModerator_ShouldReturn200() throws Exception {
        mockMvc.perform(put("/api/forum/topics/" + topic.getId() + "/pin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken))
                .andExpect(status().isOk());

        assertTrue(topicRepository.findById(topic.getId()).get().getIsPinned());
    }

    @Test
    @DisplayName("PUT /topics/{id}/pin - As Breeder, should return 403 Forbidden")
    void pinTopic_AsBreeder_ShouldReturn403() throws Exception {
        mockMvc.perform(put("/api/forum/topics/" + topic.getId() + "/pin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken))
                .andExpect(status().isForbidden());

        assertFalse(topicRepository.findById(topic.getId()).get().getIsPinned());
    }


    @Test
    @DisplayName("PUT /topics/{id}/lock - As Breeder, should return 403 Forbidden")
    void lockTopic_AsBreeder_ShouldReturn403() throws Exception {
        mockMvc.perform(put("/api/forum/topics/" + topic.getId() + "/lock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken))
                .andExpect(status().isForbidden());

        assertFalse(topicRepository.findById(topic.getId()).get().getIsLocked());
    }

    @Test
    @DisplayName("DELETE /topics/{id} - As Other Breeder, should return 403 Forbidden")
    void deleteTopic_AsOtherBreeder_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/forum/topics/" + topic.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherBreederToken))
                .andExpect(status().isForbidden());

        assertTrue(topicRepository.existsById(topic.getId()));
    }


    @Test
    @DisplayName("GET /topics/9999 - Should return 404 Not Found for non-existent topic")
    void getTopic_WhenTopicDoesNotExist_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/forum/topics/9999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /topics/9999 - Should return 404 Not Found for non-existent topic")
    void deleteTopic_WhenTopicDoesNotExist_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/forum/topics/9999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /topics - As Moderator, should save new topic and return 201 Created")
    void createTopic_AsModerator_ShouldReturn201() throws Exception {
        long initialTopicCount = topicRepository.count();
        ForumTopicRequest request = new ForumTopicRequest(category.getId(), "Temat Moderatora", "Treść stworzona przez moderatora");

        mockMvc.perform(post("/api/forum/topics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(initialTopicCount + 1, topicRepository.count());
    }

    @Test
    @DisplayName("PUT /topics/{id}/pin - As Administrator, should change pin status to true and return 200 OK")
    void pinTopic_AsAdmin_ShouldReturn200() throws Exception {
        mockMvc.perform(put("/api/forum/topics/" + topic.getId() + "/pin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertTrue(topicRepository.findById(topic.getId()).get().getIsPinned());
    }
}