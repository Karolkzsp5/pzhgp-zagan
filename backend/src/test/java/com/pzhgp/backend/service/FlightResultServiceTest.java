package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.FlightResultFileDto;
import com.pzhgp.backend.dto.FlightResultUploadRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.exception.ResourceConflictException;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FlightPlanEntryRepository;
import com.pzhgp.backend.repository.FlightResultRepository;
import com.pzhgp.backend.repository.SectionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightResultServiceTest {

    @Mock
    private FlightResultRepository flightResultRepository;

    @Mock
    private FlightPlanEntryRepository flightPlanEntryRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private BreederRepository breederRepository;

    @InjectMocks
    private FlightResultService flightResultService;

    private Breeder administrator;
    private Breeder moderator;
    private FlightPlanEntry flight;
    private Section zagan;
    private Section chotkow;

    @BeforeEach
    void setUp() {
        administrator = breeder(1L, "admin@test.pl", Role.ADMINISTRATOR);
        moderator = breeder(2L, "moderator@test.pl", Role.MODERATOR);

        FlightPlan plan = new FlightPlan();
        plan.setId(10L);
        plan.setYear(2026);

        flight = new FlightPlanEntry();
        flight.setId(100L);
        flight.setFlightPlan(plan);
        flight.setPigeonAgeGroup(PigeonAgeGroup.ADULT);
        flight.setScheduledDate(java.time.LocalDate.of(2026, 4, 26));
        flight.setLocation("Dahme");
        flight.setDistanceKm(130);
        flight.setCategory("A");
        flight.setListType("Oddziałowa");

        zagan = new Section(1L, "Żagań", 1);
        chotkow = new Section(3L, "Chotków", 3);
    }

    @Test
    @DisplayName("Administrator can upload Branch results and raw text is preserved")
    void uploadsBranchResult() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));
        when(flightResultRepository.existsByFlightPlanEntryIdAndScope(flight.getId(), FlightResultScope.BRANCH)).thenReturn(false);

        when(flightResultRepository.save(any(FlightResult.class))).thenAnswer(invocation -> {
            FlightResult saved = invocation.getArgument(0);
            saved.setId(500L);
            return saved;
        });

        String content = """
                PZHGP Oddział Żagań
                Data lotu: 02.05.2026
                Oficjalne wyniki lotu
                """;

        MockMultipartFile file = textFile("wyniki.txt", content);

        Long id = flightResultService.uploadResult(
                flight.getId(),
                file,
                new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                administrator.getEmail()
        );

        assertEquals(500L, id);

        ArgumentCaptor<FlightResult> captor = ArgumentCaptor.forClass(FlightResult.class);
        verify(flightResultRepository).save(captor.capture());

        FlightResult saved = captor.getValue();

        assertSame(flight, saved.getFlightPlanEntry());
        assertEquals(FlightResultScope.BRANCH, saved.getScope());
        assertNull(saved.getSection());
        assertEquals("wyniki.txt", saved.getOriginalFileName());
        assertEquals(content, saved.getContent());
        assertSame(administrator, saved.getUploadedBy());
    }

    @Test
    @DisplayName("Administrator can upload results for a selected Section")
    void uploadsSectionResult() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));
        when(sectionRepository.findById(chotkow.getId())).thenReturn(Optional.of(chotkow));
        when(flightResultRepository.existsByFlightPlanEntryIdAndScopeAndSectionId(
                flight.getId(), FlightResultScope.SECTION, chotkow.getId())).thenReturn(false);

        when(flightResultRepository.save(any(FlightResult.class))).thenAnswer(invocation -> {
            FlightResult saved = invocation.getArgument(0);
            saved.setId(501L);
            return saved;
        });

        Long id = flightResultService.uploadResult(
                flight.getId(),
                textFile("sekcja3.txt", "Wyniki Sekcji 3"),
                new FlightResultUploadRequest(FlightResultScope.SECTION, chotkow.getId()),
                administrator.getEmail()
        );

        assertEquals(501L, id);

        ArgumentCaptor<FlightResult> captor = ArgumentCaptor.forClass(FlightResult.class);
        verify(flightResultRepository).save(captor.capture());

        FlightResult saved = captor.getValue();

        assertEquals(FlightResultScope.SECTION, saved.getScope());
        assertSame(chotkow, saved.getSection());
        assertEquals("sekcja3.txt", saved.getOriginalFileName());
        assertEquals("Wyniki Sekcji 3", saved.getContent());
        assertSame(administrator, saved.getUploadedBy());
    }

    @Test
    @DisplayName("Directory path is removed from the original uploaded file name")
    void sanitizesOriginalFileName() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));

        when(flightResultRepository.save(any(FlightResult.class))).thenAnswer(invocation -> {
            FlightResult saved = invocation.getArgument(0);
            saved.setId(500L);
            return saved;
        });

        flightResultService.uploadResult(
                flight.getId(),
                textFile("C:\\uploads\\2026\\wyniki.txt", "Treść"),
                new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                administrator.getEmail()
        );

        ArgumentCaptor<FlightResult> captor = ArgumentCaptor.forClass(FlightResult.class);
        verify(flightResultRepository).save(captor.capture());

        assertEquals("wyniki.txt", captor.getValue().getOriginalFileName());
    }

    @Test
    @DisplayName("TXT extension is accepted regardless of letter case")
    void acceptsUppercaseTxtExtension() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));

        when(flightResultRepository.save(any(FlightResult.class))).thenAnswer(invocation -> {
            FlightResult saved = invocation.getArgument(0);
            saved.setId(500L);
            return saved;
        });

        assertDoesNotThrow(() -> flightResultService.uploadResult(
                flight.getId(),
                textFile("WYNIKI.TXT", "Treść"),
                new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                administrator.getEmail()
        ));

        verify(flightResultRepository).save(any(FlightResult.class));
    }

    @Test
    @DisplayName("Missing result file is rejected")
    void rejectsMissingFile() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        null,
                        new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                        administrator.getEmail()
                ));

        assertEquals("Nie wybrano pliku z wynikami lotu.", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Empty result file is rejected")
    void rejectsEmptyFile() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "wyniki.txt",
                "text/plain",
                new byte[0]
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        file,
                        new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                        administrator.getEmail()
                ));

        assertEquals("Nie wybrano pliku z wynikami lotu.", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Result file larger than 5 megabytes is rejected")
    void rejectsOversizedFile() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(5L * 1024 * 1024 + 1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        file,
                        new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                        administrator.getEmail()
                ));

        assertTrue(exception.getMessage().contains("5 MB"));
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("File with an extension other than TXT is rejected")
    void rejectsNonTxtFile() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "wyniki.pdf",
                "application/pdf",
                "Treść".getBytes(StandardCharsets.UTF_8)
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        file,
                        new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                        administrator.getEmail()
                ));

        assertEquals("Dozwolone są wyłącznie pliki z rozszerzeniem .txt.", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Missing result metadata is rejected")
    void rejectsMissingMetadata() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        textFile("wyniki.txt", "Treść"),
                        null,
                        administrator.getEmail()
                ));

        assertEquals("Rodzaj wyników jest wymagany.", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Result metadata without scope is rejected")
    void rejectsMissingScope() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        textFile("wyniki.txt", "Treść"),
                        new FlightResultUploadRequest(null, null),
                        administrator.getEmail()
                ));

        assertEquals("Rodzaj wyników jest wymagany.", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Branch results cannot reference a Section")
    void rejectsSectionForBranchResult() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        textFile("wyniki.txt", "Treść"),
                        new FlightResultUploadRequest(FlightResultScope.BRANCH, zagan.getId()),
                        administrator.getEmail()
                ));

        assertEquals("Wyniki oddziałowe nie mogą być przypisane do konkretnej sekcji.", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Second Branch result for the same flight is rejected")
    void rejectsDuplicateBranchResult() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));
        when(flightResultRepository.existsByFlightPlanEntryIdAndScope(
                flight.getId(), FlightResultScope.BRANCH)).thenReturn(true);

        ResourceConflictException exception = assertThrows(ResourceConflictException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        textFile("wyniki.txt", "Treść"),
                        new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                        administrator.getEmail()
                ));

        assertEquals("Wyniki oddziałowe dla tego lotu zostały już wgrane.", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Section result requires a Section identifier")
    void rejectsSectionResultWithoutSectionId() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        textFile("wyniki.txt", "Treść"),
                        new FlightResultUploadRequest(FlightResultScope.SECTION, null),
                        administrator.getEmail()
                ));

        assertEquals("Dla wyników sekcyjnych należy wybrać sekcję.", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Section result is rejected when selected Section does not exist")
    void rejectsMissingSection() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        textFile("wyniki.txt", "Treść"),
                        new FlightResultUploadRequest(FlightResultScope.SECTION, 99L),
                        administrator.getEmail()
                ));

        assertEquals("Nie znaleziono sekcji o ID: 99", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Second result for the same Section and flight is rejected")
    void rejectsDuplicateSectionResult() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));
        when(sectionRepository.findById(chotkow.getId())).thenReturn(Optional.of(chotkow));
        when(flightResultRepository.existsByFlightPlanEntryIdAndScopeAndSectionId(
                flight.getId(), FlightResultScope.SECTION, chotkow.getId())).thenReturn(true);

        ResourceConflictException exception = assertThrows(ResourceConflictException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        textFile("sekcja3.txt", "Treść"),
                        new FlightResultUploadRequest(FlightResultScope.SECTION, chotkow.getId()),
                        administrator.getEmail()
                ));

        assertEquals("Wyniki tej sekcji dla wybranego lotu zostały już wgrane.", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Text file containing only whitespace is rejected")
    void rejectsBlankTextContent() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        textFile("wyniki.txt", "   \n\t   "),
                        new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                        administrator.getEmail()
                ));

        assertEquals("Przesłany plik wyników nie zawiera żadnej treści.", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Text file containing a NUL character is rejected")
    void rejectsBinaryLikeContent() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        textFile("wyniki.txt", "ABC\0DEF"),
                        new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                        administrator.getEmail()
                ));

        assertEquals("Przesłany plik nie jest prawidłowym plikiem tekstowym.", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("IOException while reading result file is converted to IllegalArgumentException")
    void convertsIoFailureToIllegalArgumentException() throws IOException {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(flight.getId())).thenReturn(Optional.of(flight));

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getOriginalFilename()).thenReturn("wyniki.txt");
        when(file.getBytes()).thenThrow(new IOException("Disk read error"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        file,
                        new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                        administrator.getEmail()
                ));

        assertEquals("Nie udało się odczytać przesłanego pliku z wynikami.", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Moderator cannot upload flight results")
    void rejectsUploadByModerator() {
        when(breederRepository.findByEmail(moderator.getEmail())).thenReturn(Optional.of(moderator));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        textFile("wyniki.txt", "Treść"),
                        new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                        moderator.getEmail()
                ));

        assertEquals("Brak uprawnień. Wynikami lotów może zarządzać wyłącznie administrator.", exception.getMessage());
        verifyNoInteractions(flightPlanEntryRepository);
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Uploading results fails when requesting user does not exist")
    void rejectsUploadByMissingUser() {
        when(breederRepository.findByEmail("ghost@test.pl")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                flightResultService.uploadResult(
                        flight.getId(),
                        textFile("wyniki.txt", "Treść"),
                        new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                        "ghost@test.pl"
                ));

        assertEquals("Nie znaleziono użytkownika.", exception.getMessage());
        verifyNoInteractions(flightPlanEntryRepository);
    }

    @Test
    @DisplayName("Uploading results for a missing flight throws EntityNotFoundException")
    void rejectsResultForMissingFlight() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                flightResultService.uploadResult(
                        999L,
                        textFile("wyniki.txt", "Treść"),
                        new FlightResultUploadRequest(FlightResultScope.BRANCH, null),
                        administrator.getEmail()
                ));

        assertEquals("Nie znaleziono lotu w planie o ID: 999", exception.getMessage());
        verify(flightResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Stored result file is returned without modifying its content")
    void returnsResultFile() {
        FlightResult result = new FlightResult();
        result.setId(500L);
        result.setOriginalFileName("wyniki.txt");
        result.setContent("Oryginalna treść\nDruga linia");

        when(flightResultRepository.findById(result.getId())).thenReturn(Optional.of(result));

        FlightResultFileDto file = flightResultService.getResultFile(result.getId());

        assertEquals("wyniki.txt", file.originalFileName());
        assertEquals("Oryginalna treść\nDruga linia", file.content());
    }

    @Test
    @DisplayName("Requesting a missing result file throws EntityNotFoundException")
    void rejectsMissingResultFile() {
        when(flightResultRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> flightResultService.getResultFile(999L));
        assertEquals("Nie znaleziono wyników lotu o ID: 999", exception.getMessage());
    }

    @Test
    @DisplayName("Administrator can delete uploaded flight results")
    void deletesResult() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));

        FlightResult result = new FlightResult();
        result.setId(500L);

        when(flightResultRepository.findById(result.getId())).thenReturn(Optional.of(result));

        flightResultService.deleteResult(result.getId(), administrator.getEmail());

        verify(flightResultRepository).delete(result);
    }

    @Test
    @DisplayName("Deleting a missing flight result throws EntityNotFoundException")
    void rejectsDeletingMissingResult() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightResultRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> flightResultService.deleteResult(999L, administrator.getEmail()));

        assertEquals("Nie znaleziono wyników lotu o ID: 999", exception.getMessage());
        verify(flightResultRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Moderator cannot delete flight results")
    void rejectsDeletingResultByModerator() {
        when(breederRepository.findByEmail(moderator.getEmail())).thenReturn(Optional.of(moderator));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> flightResultService.deleteResult(500L, moderator.getEmail()));
        assertEquals("Brak uprawnień. Wynikami lotów może zarządzać wyłącznie administrator.", exception.getMessage());

        verify(flightResultRepository, never()).findById(any());
        verify(flightResultRepository, never()).delete(any());
    }

    private Breeder breeder(Long id, String email, Role role) {
        Breeder breeder = new Breeder();
        breeder.setId(id);
        breeder.setEmail(email);
        breeder.setRole(role);
        return breeder;
    }

    private MockMultipartFile textFile(String fileName, String content) {
        return new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}