package com.pzhgp.backend.controller;

import tools.jackson.databind.json.JsonMapper;
import com.pzhgp.backend.dto.FlightResultUploadRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FlightPlanEntryRepository;
import com.pzhgp.backend.repository.FlightPlanRepository;
import com.pzhgp.backend.repository.FlightResultRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Flight Result Integration Tests")
class FlightResultIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlightResultRepository flightResultRepository;

    @Autowired
    private FlightPlanRepository flightPlanRepository;

    @Autowired
    private FlightPlanEntryRepository flightPlanEntryRepository;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JsonMapper objectMapper;

    private Section sectionOne;
    private Section sectionThree;

    private Breeder administrator;
    private Breeder moderator;
    private Breeder breeder;

    private FlightPlan plan;
    private FlightPlanEntry flight;

    private String adminToken;
    private String moderatorToken;
    private String breederToken;

    @BeforeEach
    void setUp() {
        sectionOne = new Section();
        sectionOne.setName("Flight Result Żagań");
        sectionOne.setSortOrder(1);
        sectionRepository.save(sectionOne);

        sectionThree = new Section();
        sectionThree.setName("Flight Result Chotków");
        sectionThree.setSortOrder(3);
        sectionRepository.save(sectionThree);

        administrator = createRealUser("result-admin@test.pl", "944444444", Role.ADMINISTRATOR);
        moderator = createRealUser("result-moderator@test.pl", "955555555", Role.MODERATOR);
        breeder = createRealUser("result-breeder@test.pl", "966666666", Role.BREEDER);

        adminToken = jwtService.generateToken(administrator);
        moderatorToken = jwtService.generateToken(moderator);
        breederToken = jwtService.generateToken(breeder);

        plan = new FlightPlan();
        plan.setYear(2026);
        plan = flightPlanRepository.save(plan);

        flight = new FlightPlanEntry();
        flight.setFlightPlan(plan);
        flight.setPigeonAgeGroup(PigeonAgeGroup.ADULT);
        flight.setScheduledDate(LocalDate.of(2026, 4, 26));
        flight.setLocation("Dahme");
        flight.setDistanceKm(130);
        flight.setCategory("A");
        flight.setListType("Oddziałowa");
        flight = flightPlanEntryRepository.save(flight);
    }

    @Test
    @DisplayName("Unauthenticated user cannot upload, view or delete flight results")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/flight-results/999/file")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/flight-results/999")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Administrator can upload Branch results and file is stored in H2")
    void uploadsBranchResult() throws Exception {
        String content = """
                PZHGP Oddział Żagań
                Lot: Dahme
                Oficjalne wyniki
                """;

        Long resultId = uploadResult(textFile("oddzial.txt", content), new FlightResultUploadRequest(FlightResultScope.BRANCH, null));
        FlightResult saved = flightResultRepository.findById(resultId).orElseThrow();

        assertEquals(flight.getId(), saved.getFlightPlanEntry().getId());
        assertEquals(FlightResultScope.BRANCH, saved.getScope());
        assertNull(saved.getSection());
        assertEquals("oddzial.txt", saved.getOriginalFileName());
        assertEquals(content, saved.getContent());
        assertEquals(administrator.getId(), saved.getUploadedBy().getId());
        assertNotNull(saved.getUploadedAt());
    }

    @Test
    @DisplayName("Administrator can upload results for a selected Section")
    void uploadsSectionResult() throws Exception {
        Long resultId = uploadResult(
                textFile("sekcja3.txt", "Lista konkursowa Sekcji 3"),
                new FlightResultUploadRequest(FlightResultScope.SECTION, sectionThree.getId())
        );

        FlightResult saved = flightResultRepository.findById(resultId).orElseThrow();

        assertEquals(FlightResultScope.SECTION, saved.getScope());
        assertEquals(sectionThree.getId(), saved.getSection().getId());
        assertEquals("sekcja3.txt", saved.getOriginalFileName());
    }

    @Test
    @DisplayName("Moderator and Breeder cannot upload flight results")
    void rejectsUploadByNonAdministrator() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(moderatorToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(breederToken)))
                .andExpect(status().isForbidden());

        assertEquals(0, flightResultRepository.count());
    }

    @Test
    @DisplayName("Current database role is checked when Administrator JWT becomes outdated")
    void usesCurrentDatabaseRoleWhenUploadingResult() throws Exception {
        administrator.setRole(Role.BREEDER);
        breederRepository.save(administrator);

        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isForbidden());

        assertEquals(0, flightResultRepository.count());
    }

    @Test
    @DisplayName("Second Branch result for the same flight returns 409 Conflict")
    void rejectsDuplicateBranchResult() throws Exception {
        uploadResult(textFile("oddzial.txt", "Pierwsza lista"),
                new FlightResultUploadRequest(FlightResultScope.BRANCH, null)
        );

        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("oddzial2.txt", "Druga lista"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Wyniki oddziałowe dla tego lotu zostały już wgrane."));

        assertEquals(1, flightResultRepository.count());
    }

    @Test
    @DisplayName("Second result for the same Section and flight returns 409 Conflict")
    void rejectsDuplicateSectionResult() throws Exception {
        uploadResult(textFile("sekcja3.txt", "Pierwsza lista"),
                new FlightResultUploadRequest(FlightResultScope.SECTION, sectionThree.getId())
        );

        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("sekcja3-v2.txt", "Druga lista"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.SECTION, sectionThree.getId())))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Wyniki tej sekcji dla wybranego lotu zostały już wgrane."));

        assertEquals(1, flightResultRepository.count());
    }

    @Test
    @DisplayName("Branch results cannot contain Section identifier")
    void rejectsSectionIdForBranchResult() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, sectionOne.getId())))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Wyniki oddziałowe nie mogą być przypisane do konkretnej sekcji."));
    }

    @Test
    @DisplayName("Section result requires Section identifier")
    void rejectsSectionResultWithoutSectionId() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.SECTION, null)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Dla wyników sekcyjnych należy wybrać sekcję."));
    }

    @Test
    @DisplayName("Selecting non-existent Section returns 404 Not Found")
    void rejectsMissingSection() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.SECTION, 999999L)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Uploading result for non-existent flight returns 404 Not Found")
    void rejectsMissingFlight() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/999999")
                        .file(textFile("wyniki.txt", "Treść"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("File with extension other than TXT returns 400 Bad Request")
    void rejectsNonTxtFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "wyniki.pdf",
                "application/pdf",
                "Treść".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(file)
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Dozwolone są wyłącznie pliki z rozszerzeniem .txt."));
    }

    @Test
    @DisplayName("TXT file containing only whitespace returns 400 Bad Request")
    void rejectsBlankTextFile() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "   \n\t   "))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Przesłany plik wyników nie zawiera żadnej treści."));
    }

    @Test
    @DisplayName("TXT file containing NUL character returns 400 Bad Request")
    void rejectsBinaryLikeTextFile() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "ABC\0DEF"))
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Przesłany plik nie jest prawidłowym plikiem tekstowym."));
    }

    @Test
    @DisplayName("Result file larger than 5 megabytes returns 400 Bad Request")
    void rejectsOversizedFile() throws Exception {
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "wyniki.txt",
                "text/plain",
                oversized
        );

        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(file)
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Plik jest za duży. Maksymalny rozmiar pliku z wynikami to 5 MB."));
    }

    @Test
    @DisplayName("Missing multipart file part returns 400 Bad Request")
    void rejectsMissingFilePart() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(metadataPart(new FlightResultUploadRequest(FlightResultScope.BRANCH, null)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Żądanie jest niekompletne — brakuje części \"file\"."));
    }

    @Test
    @DisplayName("Missing multipart metadata part returns 400 Bad Request")
    void rejectsMissingMetadataPart() throws Exception {
        mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(textFile("wyniki.txt", "Treść"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Żądanie jest niekompletne — brakuje części \"metadata\"."));
    }

    @Test
    @DisplayName("Stored result file is returned inline as UTF-8 plain text")
    void returnsResultFile() throws Exception {
        String originalContent = "PZHGP Żagań\nZażółć gęślą jaźń\n12345";

        Long resultId = uploadResult(
                textFile("wyniki-dahme.txt", originalContent),
                new FlightResultUploadRequest(FlightResultScope.BRANCH, null)
        );

        mockMvc.perform(get("/api/flight-results/" + resultId + "/file")
                        .header(HttpHeaders.AUTHORIZATION, bearer(breederToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(new MediaType("text", "plain", StandardCharsets.UTF_8)))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("wyniki-dahme.txt")))
                .andExpect(content().string(originalContent));
    }

    @Test
    @DisplayName("Administrator can delete uploaded result")
    void deletesResult() throws Exception {
        Long resultId = uploadResult(textFile("wyniki.txt", "Treść"),
                new FlightResultUploadRequest(FlightResultScope.BRANCH, null)
        );

        mockMvc.perform(delete("/api/flight-results/" + resultId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        assertTrue(flightResultRepository.findById(resultId).isEmpty());
    }

    @Test
    @DisplayName("Moderator cannot delete uploaded result")
    void rejectsDeletingResultByModerator() throws Exception {
        Long resultId = uploadResult(textFile("wyniki.txt", "Treść"),
                new FlightResultUploadRequest(FlightResultScope.BRANCH, null)
        );

        mockMvc.perform(delete("/api/flight-results/" + resultId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(moderatorToken)))
                .andExpect(status().isForbidden());

        assertTrue(flightResultRepository.findById(resultId).isPresent());
    }

    @Test
    @DisplayName("Plan details return Branch result first and Sections in configured order")
    void returnsResultsInCorrectOrder() throws Exception {
        uploadResult(textFile("sekcja3.txt", "Sekcja 3"), new FlightResultUploadRequest(FlightResultScope.SECTION, sectionThree.getId()));
        uploadResult(textFile("oddzial.txt", "Oddział"), new FlightResultUploadRequest(FlightResultScope.BRANCH, null));
        uploadResult(textFile("sekcja1.txt", "Sekcja 1"), new FlightResultUploadRequest(FlightResultScope.SECTION, sectionOne.getId()));

        mockMvc.perform(get("/api/flight-plans/2026")
                        .header(HttpHeaders.AUTHORIZATION, bearer(breederToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adultFlights[0].results.length()").value(3))
                .andExpect(jsonPath("$.adultFlights[0].results[0].scope").value("BRANCH"))
                .andExpect(jsonPath("$.adultFlights[0].results[0].originalFileName").value("oddzial.txt"))
                .andExpect(jsonPath("$.adultFlights[0].results[1].scope").value("SECTION"))
                .andExpect(jsonPath("$.adultFlights[0].results[1].sectionSortOrder").value(1))
                .andExpect(jsonPath("$.adultFlights[0].results[2].sectionSortOrder").value(3));
    }

    private Breeder createRealUser(String email, String phoneNumber, Role role) {
        Breeder user = new Breeder();
        user.setEmail(email);
        user.setName("Test");
        user.setSurname("User");
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash("hashed");
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setSection(sectionOne);
        return breederRepository.save(user);
    }

    private MockMultipartFile textFile(String fileName, String content) {
        return new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private MockMultipartFile metadataPart(FlightResultUploadRequest request) throws IOException {
        return new MockMultipartFile(
                "metadata",
                "metadata",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );
    }

    private Long uploadResult(MockMultipartFile file, FlightResultUploadRequest metadata) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/flight-results/entries/" + flight.getId())
                        .file(file)
                        .file(metadataPart(metadata))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}