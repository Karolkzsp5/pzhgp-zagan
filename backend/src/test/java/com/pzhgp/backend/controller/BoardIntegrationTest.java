package com.pzhgp.backend.controller;

import tools.jackson.databind.json.JsonMapper;
import com.pzhgp.backend.dto.BoardMemberRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.exception.ResourceConflictException;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.pzhgp.backend.support.CookieAuthTestSupport.cookieAuth;
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

    @Autowired
    private JsonMapper objectMapper;

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

        Breeder admin = createRealUser("admin@test.pl", "111111111", Role.ADMINISTRATOR, sectionZagan);
        Breeder moderator = createRealUser("mod@test.pl", "222222222", Role.MODERATOR, sectionZagan);

        breederZagan = createRealUser("hodowca_zagan@test.pl", "333333333", Role.BREEDER, sectionZagan);
        breederZagan2 = createRealUser("hodowca2_zagan@test.pl", "444444444", Role.BREEDER, sectionZagan);
        breederWymiarki = createRealUser("hodowca_wymiarki@test.pl", "555555555", Role.BREEDER, sectionWymiarki);

        adminToken = jwtService.generateToken(admin);
        modToken = jwtService.generateToken(moderator);
    }

    private Breeder createRealUser(String email, String phoneNumber, Role role, Section section) {
        Breeder breeder = new Breeder();
        breeder.setEmail(email);
        breeder.setRole(role);
        breeder.setStatus(AccountStatus.ACTIVE);
        breeder.setName("Jan");
        breeder.setSurname("Kowalski");
        breeder.setPhoneNumber(phoneNumber);
        breeder.setPasswordHash("hashed");
        breeder.setSection(section);
        return breederRepository.save(breeder);
    }

    @Test
    @DisplayName("Should return board members with correctly mapped breeder and manual-person details")
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
    @DisplayName("Should return 401 when an anonymous user creates a board member")
    void shouldDenyAccessToCreateBoardMemberForAnonymousUser() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.PREZES, null, breederZagan.getId(), null, null, null);

        mockMvc.perform(post("/api/board").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 403 when a moderator creates a board member")
    void shouldDenyAccessToCreateBoardMemberForModerator() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.PREZES, null, breederZagan.getId(), null, null, null);

        mockMvc.perform(post("/api/board")
                        .with(cookieAuth(modToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should create a section president and return 201")
    void shouldCreateSectionPrezesSuccessfully() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.PREZES, sectionZagan.getId(), breederZagan.getId(), null, null, null);

        mockMvc.perform(post("/api/board")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(1, boardMemberRepository.findAll().size());
        assertEquals(BoardRole.PREZES, boardMemberRepository.findAll().getFirst().getRole());
    }

    @Test
    @DisplayName("Should create a board member using manually entered personal details")
    void shouldCreateManualPersonSuccessfully() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.WICEPREZES_DS_GOSPODARCZYCH, null, null, "Tomasz", "Zewnętrzny", null);

        mockMvc.perform(post("/api/board")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        BoardMember saved = boardMemberRepository.findAll().getFirst();
        assertEquals("Tomasz", saved.getCustomName());
        assertEquals("Zewnętrzny", saved.getCustomSurname());
    }

    @Test
    @DisplayName("Should create a manually entered board member with a phone number")
    void shouldCreateManualPersonWithPhoneSuccessfully() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.SEKRETARZ, null, null, "Tomasz", "Zewnętrzny", "987654321");

        mockMvc.perform(post("/api/board")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        BoardMember saved = boardMemberRepository.findAll().getFirst();
        assertEquals("987654321", saved.getContactPhone());
    }

    @Test
    @DisplayName("Should return 400 when assigning a vice president to a section board")
    void shouldRejectVicePresidentInSection() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.WICEPREZES_DS_LOTOWYCH, sectionZagan.getId(), breederZagan.getId(), null, null, null);

        mockMvc.perform(post("/api/board")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(IllegalArgumentException.class, result.getResolvedException()));

        assertEquals(0, boardMemberRepository.count());
    }

    @Test
    @DisplayName("Should return 400 when assigning a treasurer to the branch board")
    void shouldRejectTreasurerInBranch() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.SKARBNIK, null, breederZagan.getId(), null, null, null);

        mockMvc.perform(post("/api/board")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(IllegalArgumentException.class, result.getResolvedException()));
    }

    @Test
    @DisplayName("Should return 409 when assigning a second president to the same board")
    void shouldRejectSecondPrezesInSameBoard() throws Exception {
        BoardMember firstPrezes = new BoardMember();
        firstPrezes.setRole(BoardRole.PREZES);
        firstPrezes.setManagedSection(sectionZagan);
        firstPrezes.setBreeder(breederZagan);
        boardMemberRepository.save(firstPrezes);

        BoardMemberRequest request = new BoardMemberRequest(BoardRole.PREZES, sectionZagan.getId(), breederZagan2.getId(), null, null, null);

        mockMvc.perform(post("/api/board")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(result -> assertInstanceOf(ResourceConflictException.class, result.getResolvedException()));
    }

    @Test
    @DisplayName("Should return 409 when assigning the same board role twice to one breeder")
    void shouldRejectDuplicateRoleForSameBreeder() throws Exception {
        BoardMember existingMember = new BoardMember();
        existingMember.setRole(BoardRole.CZLONEK_ZARZADU);
        existingMember.setManagedSection(null);
        existingMember.setBreeder(breederZagan);
        boardMemberRepository.save(existingMember);

        BoardMemberRequest request = new BoardMemberRequest(BoardRole.CZLONEK_ZARZADU, null, breederZagan.getId(), null, null, null);

        mockMvc.perform(post("/api/board")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(result -> assertInstanceOf(ResourceConflictException.class, result.getResolvedException()));
    }

    @Test
    @DisplayName("Should return 400 when assigning a breeder to another section's board")
    void shouldRejectBreederFromDifferentSection() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.PREZES, sectionZagan.getId(), breederWymiarki.getId(), null, null, null);

        mockMvc.perform(post("/api/board")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(IllegalArgumentException.class, result.getResolvedException()));
    }

    @Test
    @DisplayName("Should return 400 when creating a board member with a non-positive breeder ID")
    void shouldRejectNonPositiveBreederId() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.CZLONEK_ZARZADU, null, -1L, null, null, null);

        mockMvc.perform(post("/api/board")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("ID hodowcy musi być liczbą dodatnią"));
    }

    @Test
    @DisplayName("Should reject a board member request combining a breeder account with manual personal data")
    void shouldRejectBreederCombinedWithManualPersonData() throws Exception {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.CZLONEK_ZARZADU, null, breederZagan.getId(), "Jan", "Ręczny", null);

        mockMvc.perform(post("/api/board")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Wymagane jest wybranie konta hodowcy ALBO podanie imienia i nazwiska dla osoby, która nie ma konta w systemie."));

        assertEquals(0, boardMemberRepository.count());
    }

    @Test
    @DisplayName("Should return 409 when assigning a pending breeder to a board")
    void shouldRejectPendingBreeder() throws Exception {
        breederZagan.setStatus(AccountStatus.PENDING);
        breederRepository.save(breederZagan);

        BoardMemberRequest request = new BoardMemberRequest(BoardRole.CZLONEK_ZARZADU, null, breederZagan.getId(), null, null, null);

        mockMvc.perform(post("/api/board")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Do zarządu można przypisać wyłącznie aktywne konto hodowcy."));

        assertEquals(0, boardMemberRepository.count());
    }

    @Test
    @DisplayName("Should return 409 when assigning a blocked breeder to a board")
    void shouldRejectBlockedBreeder() throws Exception {
        breederZagan.setStatus(AccountStatus.BLOCKED);
        breederRepository.save(breederZagan);

        BoardMemberRequest request = new BoardMemberRequest(BoardRole.CZLONEK_ZARZADU, null, breederZagan.getId(), null, null, null);

        mockMvc.perform(post("/api/board")
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Do zarządu można przypisać wyłącznie aktywne konto hodowcy."));

        assertEquals(0, boardMemberRepository.count());
    }

    @Test
    @DisplayName("Should save changes when updating an existing board member")
    void shouldUpdateExistingMemberSuccessfully() throws Exception {
        BoardMember existingMember = new BoardMember();
        existingMember.setRole(BoardRole.SEKRETARZ);
        existingMember.setManagedSection(sectionZagan);
        existingMember.setBreeder(breederZagan);
        existingMember = boardMemberRepository.save(existingMember);

        BoardMemberRequest request = new BoardMemberRequest(BoardRole.SKARBNIK, sectionZagan.getId(), breederZagan.getId(), null, null, "111222333");

        mockMvc.perform(put("/api/board/" + existingMember.getId())
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        BoardMember updated = boardMemberRepository.findById(existingMember.getId()).orElseThrow();
        assertEquals(BoardRole.SKARBNIK, updated.getRole());
        assertEquals("111222333", updated.getContactPhone());
    }

    @Test
    @DisplayName("Should reject updating a board member to reference a blocked breeder")
    void shouldRejectBlockedBreederOnUpdate() throws Exception {
        BoardMember existingMember = new BoardMember();
        existingMember.setRole(BoardRole.CZLONEK_ZARZADU);
        existingMember.setBreeder(breederZagan);
        existingMember = boardMemberRepository.save(existingMember);

        breederZagan2.setStatus(AccountStatus.BLOCKED);
        breederRepository.save(breederZagan2);

        BoardMemberRequest request = new BoardMemberRequest(BoardRole.CZLONEK_ZARZADU, null, breederZagan2.getId(), null, null, null);

        mockMvc.perform(put("/api/board/" + existingMember.getId())
                        .with(cookieAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Do zarządu można przypisać wyłącznie aktywne konto hodowcy."));

        BoardMember unchanged = boardMemberRepository.findById(existingMember.getId()).orElseThrow();
        assertEquals(breederZagan.getId(), unchanged.getBreeder().getId());
    }

    @Test
    @DisplayName("Should allow an administrator to delete a board member")
    void shouldDeleteBoardMemberSuccessfully() throws Exception {
        BoardMember member = new BoardMember();
        member.setRole(BoardRole.PREZES);
        member.setManagedSection(sectionZagan);
        member.setBreeder(breederZagan);
        member = boardMemberRepository.save(member);

        mockMvc.perform(delete("/api/board/" + member.getId()).with(cookieAuth(adminToken))).andExpect(status().isNoContent());
        assertTrue(boardMemberRepository.findById(member.getId()).isEmpty());
    }
}