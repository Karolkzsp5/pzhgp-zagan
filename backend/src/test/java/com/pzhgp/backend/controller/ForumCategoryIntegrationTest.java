package com.pzhgp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.ForumCategoryRepository;
import com.pzhgp.backend.repository.ForumThreadRepository;
import com.pzhgp.backend.repository.SectionRepository;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Forum Category Integration Tests")
class ForumCategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ForumCategoryRepository categoryRepository;

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ForumCategory adminCategory;
    private ForumCategory modCategory;
    private ForumCategory anotherAdminCategory;

    private Breeder admin;
    private String adminToken;
    private String modToken;
    private String breederToken;
    private String anotherAdminToken;

    @BeforeEach
    void setUp() {
        Section section = new Section(null, "Sekcja Testowa", 1);
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

        Breeder anotherAdmin = new Breeder();
        anotherAdmin.setEmail("admin2@test.pl");
        anotherAdmin.setName("Drugi");
        anotherAdmin.setSurname("Admin");
        anotherAdmin.setPhoneNumber("999999999");
        anotherAdmin.setPasswordHash("hashed9");
        anotherAdmin.setRole(Role.ADMINISTRATOR);
        anotherAdmin.setStatus(AccountStatus.ACTIVE);
        anotherAdmin.setSection(section);
        breederRepository.save(anotherAdmin);
        anotherAdminToken = jwtService.generateToken(anotherAdmin);

        Breeder moderator = new Breeder();
        moderator.setEmail("mod@test.pl");
        moderator.setName("Moderator");
        moderator.setSurname("Testowy");
        moderator.setPhoneNumber("222222222");
        moderator.setPasswordHash("hashed2");
        moderator.setRole(Role.MODERATOR);
        moderator.setStatus(AccountStatus.ACTIVE);
        moderator.setSection(section);
        breederRepository.save(moderator);
        modToken = jwtService.generateToken(moderator);

        Breeder standardBreeder = new Breeder();
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

        adminCategory = new ForumCategory();
        adminCategory.setName("Wystawy i loty");
        adminCategory.setAuthor(admin);
        adminCategory.setSortOrder(1);
        categoryRepository.save(adminCategory);

        modCategory = new ForumCategory();
        modCategory.setName("Kategoria Moderatora");
        modCategory.setAuthor(moderator);
        modCategory.setSortOrder(2);
        categoryRepository.save(modCategory);

        anotherAdminCategory = new ForumCategory();
        anotherAdminCategory.setName("Kategoria Innego Admina");
        anotherAdminCategory.setAuthor(anotherAdmin);
        anotherAdminCategory.setSortOrder(3);
        categoryRepository.save(anotherAdminCategory);
    }

    @Test
    @DisplayName("GET /categories - Authenticated can fetch categories")
    void getAllCategories_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/forum/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + breederToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Wystawy i loty"));
    }

    @Test
    @DisplayName("GET /categories - Unauthenticated user gets 403 Forbidden")
    void getAllCategories_WhenUnauthenticated_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/forum/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /categories/{id} - Should return existing category")
    void getCategoryById_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/forum/categories/" + adminCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + breederToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wystawy i loty"));
    }

    @Test
    @DisplayName("GET /categories/{id} - Should return 404 for non-existing category")
    void getCategoryById_WhenMissing_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/forum/categories/9999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + breederToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /categories - Admin can create category and data is saved correctly in DB")
    void createCategory_AsAdmin_ShouldReturn201() throws Exception {
        long initialCount = categoryRepository.count();
        Map<String, Object> request = Map.of(
                "name", "Nowy Dział",
                "description", "Opis testowy",
                "sortOrder", 99
        );

        mockMvc.perform(post("/api/forum/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(initialCount + 1, categoryRepository.count());

        ForumCategory saved = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Nowy Dział"))
                .findFirst().get();

        assertEquals("Opis testowy", saved.getDescription());
        assertEquals(99, saved.getSortOrder());
        assertEquals(admin.getId(), saved.getAuthor().getId());
    }

    @Test
    @DisplayName("POST /categories - Should return 400 Bad Request on invalid empty data")
    void createCategory_WithInvalidData_ShouldReturn400() throws Exception {
        Map<String, Object> request = Map.of("name", "", "sortOrder", 1);

        mockMvc.perform(post("/api/forum/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /categories - Moderator can create category")
    void createCategory_AsModerator_ShouldReturn201() throws Exception {
        long initialCount = categoryRepository.count();
        Map<String, String> request = Map.of("name", "Dział Moderatora");

        mockMvc.perform(post("/api/forum/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(initialCount + 1, categoryRepository.count());
    }

    @Test
    @DisplayName("POST /categories - Breeder gets 403 Forbidden")
    void createCategory_AsBreeder_ShouldReturn403() throws Exception {
        Map<String, String> request = Map.of("name", "Próba ataku");

        mockMvc.perform(post("/api/forum/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + breederToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /categories/{id} - Moderator can update THEIR OWN category")
    void updateCategory_AsModeratorOnOwnCategory_ShouldReturn200() throws Exception {
        Map<String, String> request = Map.of("name", "Zmieniona Nazwa Mod");

        mockMvc.perform(put("/api/forum/categories/" + modCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertEquals("Zmieniona Nazwa Mod", categoryRepository.findById(modCategory.getId()).get().getName());
    }

    @Test
    @DisplayName("PUT /categories/{id} - Admin can update Moderator's category")
    void updateCategory_AsAdminOnModeratorCategory_ShouldReturn200() throws Exception {
        Map<String, String> request = Map.of("name", "Zmieniona przez Admina");

        mockMvc.perform(put("/api/forum/categories/" + modCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertEquals("Zmieniona przez Admina", categoryRepository.findById(modCategory.getId()).get().getName());
    }

    @Test
    @DisplayName("PUT /categories/{id} - Moderator attempting to update Admin's category gets 403 Forbidden")
    void updateCategory_AsModeratorOnAdminCategory_ShouldReturn403() throws Exception {
        Map<String, String> request = Map.of("name", "Włam Moderatora");

        mockMvc.perform(put("/api/forum/categories/" + adminCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertEquals("Wystawy i loty", categoryRepository.findById(adminCategory.getId()).get().getName());
    }

    @Test
    @DisplayName("PUT /categories/{id} - Admin attempting to update ANOTHER Admin's category gets 403 Forbidden")
    void updateCategory_AsAdminOnAnotherAdminCategory_ShouldReturn403() throws Exception {
        Map<String, String> request = Map.of("name", "Włam Admina");

        mockMvc.perform(put("/api/forum/categories/" + anotherAdminCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertEquals("Kategoria Innego Admina", categoryRepository.findById(anotherAdminCategory.getId()).get().getName());
    }

    @Test
    @DisplayName("PUT /categories/{id} - Breeder gets 403 Forbidden")
    void updateCategory_AsBreeder_ShouldReturn403() throws Exception {
        Map<String, String> request = Map.of("name", "Zmieniona Nazwa");

        mockMvc.perform(put("/api/forum/categories/" + adminCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + breederToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertEquals("Wystawy i loty", categoryRepository.findById(adminCategory.getId()).get().getName());
    }

    @Test
    @DisplayName("PUT /categories/9999 - Should return 404 Not Found")
    void updateCategory_WhenCategoryNotFound_ShouldReturn404() throws Exception {
        Map<String, String> request = Map.of("name", "Test");

        mockMvc.perform(put("/api/forum/categories/9999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /categories/{id} - Admin can delete empty category")
    void deleteCategory_AsAdmin_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/forum/categories/" + adminCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertTrue(categoryRepository.findById(adminCategory.getId()).isEmpty());
    }

    @Test
    @DisplayName("DELETE /categories/{id} - Should return 403 Forbidden when attempting to delete category that contains threads")
    void deleteCategory_WhenThreadsExist_ShouldReturn403() throws Exception {
        ForumThread thread = new ForumThread();
        thread.setCategory(adminCategory);
        thread.setAuthor(admin);
        thread.setTitle("Wątek blokujący");
        threadRepository.save(thread);

        mockMvc.perform(delete("/api/forum/categories/" + adminCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Nie można usunąć kategorii, która zawiera wątki."));

        assertTrue(categoryRepository.existsById(adminCategory.getId()));
    }

    @Test
    @DisplayName("DELETE /categories/{id} - Moderator gets 403 Forbidden")
    void deleteCategory_AsModerator_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/forum/categories/" + modCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken))
                .andExpect(status().isForbidden());

        assertTrue(categoryRepository.findById(modCategory.getId()).isPresent());
    }

    @Test
    @DisplayName("DELETE /categories/{id} - Breeder gets 403 Forbidden")
    void deleteCategory_AsBreeder_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/forum/categories/" + adminCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + breederToken))
                .andExpect(status().isForbidden());
    }
}