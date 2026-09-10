package com.pzhgp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pzhgp.backend.dto.BoardMemberRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BoardMemberRepository;
import com.pzhgp.backend.repository.BreederRepository;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Board Member Integration Tests")
class BoardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardMemberRepository boardMemberRepository;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Section sectionZagan;
    private Section sectionWymiarki;

    private Breeder breederZagan;
    private Breeder breederZagan2;
    private Breeder breederWymiarki;

    private String adminToken;
    private String modToken;

    @BeforeEach
    void setUp() {
        boardMemberRepository.deleteAll();
        breederRepository.deleteAll();

        List<Section> existingSections = sectionRepository.findAll();
        sectionZagan = existingSections.stream().filter(s -> s.getName().equals("Żagań")).findFirst().orElseThrow();
        sectionWymiarki = existingSections.stream().filter(s -> s.getName().equals("Wymiarki")).findFirst().orElseThrow();

        Breeder admin = createRealUser("admin@test.pl", Role.ADMINISTRATOR, sectionZagan);
        Breeder moderator = createRealUser("mod@test.pl", Role.MODERATOR, sectionZagan);

        breederZagan = createRealUser("hodowca_zagan@test.pl", Role.BREEDER, sectionZagan);
        breederZagan2 = createRealUser("hodowca2_zagan@test.pl", Role.BREEDER, sectionZagan);
        breederWymiarki = createRealUser("hodowca_wymiarki@test.pl", Role.BREEDER, sectionWymiarki);

        adminToken = "Bearer " + jwtService.generateToken(admin);
        modToken = "Bearer " + jwtService.generateToken(moderator);
    }

    private Breeder createRealUser(String email, Role role, Section section) {
        Breeder breeder = new Breeder();
        breeder.setEmail(email);
        breeder.setRole(role);
        breeder.setStatus(AccountStatus.ACTIVE);
        breeder.setName("Jan");
        breeder.setSurname("Kowalski");
        breeder.setPhoneNumber(String.valueOf(System.nanoTime()).substring(0, 9));
        breeder.setPasswordHash("hashed");
        breeder.setSection(section);
        return breederRepository.save(breeder);
    }


    @Test
    @DisplayName("POST /api/board - Should deny access for anonymous users")
    void shouldDenyAccessToCreateBoardMemberForAnonymousUser() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.PREZES, null, breederZagan.getId(), null, null, null);

        mockMvc.perform(post("/api/board")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/board - Should deny access for MODERATOR authority")
    void shouldDenyAccessToCreateBoardMemberForModerator() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.PREZES, null, breederZagan.getId(), null, null, null);

        mockMvc.perform(post("/api/board")
                        .header(HttpHeaders.AUTHORIZATION, modToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("POST /api/board - Should create PREZES in Section successfully (201 Created)")
    void shouldCreateSectionPrezesSuccessfully() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.PREZES, sectionZagan.getId(), breederZagan.getId(), null, null, null
        );

        mockMvc.perform(post("/api/board")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(1, boardMemberRepository.findAll().size());
        assertEquals(BoardRole.PREZES, boardMemberRepository.findAll().getFirst().getRole());
    }

    @Test
    @DisplayName("POST /api/board - Should reject assigning WICEPREZES to Section")
    void shouldRejectVicePresidentInSection() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.WICEPREZES_DS_LOTOWYCH, sectionZagan.getId(), breederZagan.getId(), null, null, null
        );

        mockMvc.perform(post("/api/board")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertInstanceOf(IllegalStateException.class, result.getResolvedException()));

        assertEquals(0, boardMemberRepository.count());
    }

    @Test
    @DisplayName("POST /api/board - Should reject assigning SKARBNIK to Branch (Oddział)")
    void shouldRejectTreasurerInBranch() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.SKARBNIK, null, breederZagan.getId(), null, null, null
        );

        mockMvc.perform(post("/api/board")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertInstanceOf(IllegalStateException.class, result.getResolvedException()));
    }

    @Test
    @DisplayName("POST /api/board - Should reject assigning a second PREZES to the same board")
    void shouldRejectSecondPrezesInSameBoard() throws Exception {
        BoardMember firstPrezes = new BoardMember();
        firstPrezes.setRole(BoardRole.PREZES);
        firstPrezes.setManagedSection(sectionZagan);
        firstPrezes.setBreeder(breederZagan);
        boardMemberRepository.save(firstPrezes);

        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.PREZES, sectionZagan.getId(), breederZagan2.getId(), null, null, null
        );

        mockMvc.perform(post("/api/board")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertInstanceOf(IllegalStateException.class, result.getResolvedException()));
    }

    @Test
    @DisplayName("POST /api/board - Should reject assigning the same role to the same breeder in the same board")
    void shouldRejectDuplicateRoleForSameBreeder() throws Exception {
        BoardMember existingMember = new BoardMember();
        existingMember.setRole(BoardRole.CZLONEK_ZARZADU);
        existingMember.setManagedSection(null);
        existingMember.setBreeder(breederZagan);
        boardMemberRepository.save(existingMember);

        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.CZLONEK_ZARZADU, null, breederZagan.getId(), null, null, null
        );

        mockMvc.perform(post("/api/board")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertInstanceOf(IllegalStateException.class, result.getResolvedException()));
    }

    @Test
    @DisplayName("POST /api/board - Should reject breeder if they belong to a different Section")
    void shouldRejectBreederFromDifferentSection() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.PREZES, sectionZagan.getId(), breederWymiarki.getId(), null, null, null
        );

        mockMvc.perform(post("/api/board")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertInstanceOf(IllegalStateException.class, result.getResolvedException()));
    }

    @Test
    @DisplayName("POST /api/board - Should create manual person with custom name and surname")
    void shouldCreateManualPersonSuccessfully() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.WICEPREZES_DS_GOSPODARCZYCH, null, null, "Tomasz", "Zewnętrzny", null
        );

        mockMvc.perform(post("/api/board")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        BoardMember saved = boardMemberRepository.findAll().getFirst();
        assertEquals("Tomasz", saved.getCustomName());
        assertEquals("Zewnętrzny", saved.getCustomSurname());
    }

    @Test
    @DisplayName("POST /api/board - Should create manual person with phone number")
    void shouldCreateManualPersonWithPhoneSuccessfully() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.SEKRETARZ, null, null, "Tomasz", "Zewnętrzny", "987654321"
        );

        mockMvc.perform(post("/api/board")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        BoardMember saved = boardMemberRepository.findAll().getFirst();
        assertEquals("987654321", saved.getContactPhone());
    }


    @Test
    @DisplayName("PUT /api/board/{id} - Should update existing member and reflect changes in DB")
    void shouldUpdateExistingMemberSuccessfully() throws Exception {
        BoardMember existingMember = new BoardMember();
        existingMember.setRole(BoardRole.SEKRETARZ);
        existingMember.setManagedSection(sectionZagan);
        existingMember.setBreeder(breederZagan);
        existingMember = boardMemberRepository.save(existingMember);

        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.SKARBNIK, sectionZagan.getId(), breederZagan.getId(), null, null, "111222333"
        );

        mockMvc.perform(put("/api/board/" + existingMember.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        BoardMember updated = boardMemberRepository.findById(existingMember.getId()).orElseThrow();
        assertEquals(BoardRole.SKARBNIK, updated.getRole());
        assertEquals("111222333", updated.getContactPhone());
    }

    @Test
    @DisplayName("GET /api/board - Should return correct DTO list combining system breeders and manual persons")
    void shouldReturnCorrectDtoListOnGet() throws Exception {
        BoardMember sysMember = new BoardMember();
        sysMember.setRole(BoardRole.PREZES);
        sysMember.setManagedSection(sectionZagan);
        sysMember.setBreeder(breederZagan);
        boardMemberRepository.save(sysMember);

        BoardMember manMember = new BoardMember();
        manMember.setRole(BoardRole.WICEPREZES_DS_LOTOWYCH);
        manMember.setCustomName("Piotr");
        manMember.setCustomSurname("Ręczny");
        manMember.setContactPhone("123123123");
        boardMemberRepository.save(manMember);

        mockMvc.perform(get("/api/board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("WICEPREZES_DS_LOTOWYCH"))
                .andExpect(jsonPath("$[0].firstName").value("Piotr"))
                .andExpect(jsonPath("$[0].lastName").value("Ręczny"))
                .andExpect(jsonPath("$[0].publicPhoneNumber").value("123123123"))
                .andExpect(jsonPath("$[0].managedSectionId").isEmpty())
                .andExpect(jsonPath("$[1].role").value("PREZES"))
                .andExpect(jsonPath("$[1].firstName").value("Jan"))
                .andExpect(jsonPath("$[1].lastName").value("Kowalski"))
                .andExpect(jsonPath("$[1].managedSectionName").value("Żagań"))
                .andExpect(jsonPath("$[1].breederId").value(breederZagan.getId().intValue()));
    }

    @Test
    @DisplayName("DELETE /api/board/{id} - Administrator should successfully delete board member from DB")
    void shouldDeleteBoardMemberSuccessfully() throws Exception {
        BoardMember member = new BoardMember();
        member.setRole(BoardRole.PREZES);
        member.setManagedSection(sectionZagan);
        member.setBreeder(breederZagan);
        member = boardMemberRepository.save(member);

        mockMvc.perform(delete("/api/board/" + member.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNoContent());

        assertTrue(boardMemberRepository.findById(member.getId()).isEmpty());
    }
}