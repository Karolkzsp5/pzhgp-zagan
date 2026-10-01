package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.FlightResultFileDto;
import com.pzhgp.backend.dto.FlightResultUploadRequest;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.FlightPlanEntry;
import com.pzhgp.backend.entity.FlightResult;
import com.pzhgp.backend.entity.FlightResultScope;
import com.pzhgp.backend.entity.Role;
import com.pzhgp.backend.entity.Section;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FlightPlanEntryRepository;
import com.pzhgp.backend.repository.FlightResultRepository;
import com.pzhgp.backend.repository.SectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class FlightResultService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final FlightResultRepository flightResultRepository;
    private final FlightPlanEntryRepository flightPlanEntryRepository;
    private final SectionRepository sectionRepository;
    private final BreederRepository breederRepository;

    @Transactional
    public Long uploadResult(
            Long entryId,
            MultipartFile file,
            FlightResultUploadRequest request,
            String userEmail
    ) {
        Breeder uploader = requireAdministrator(userEmail);

        FlightPlanEntry entry = flightPlanEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono lotu w planie o ID: " + entryId
                ));

        validateFile(file);

        if (request == null || request.scope() == null) {
            throw new IllegalArgumentException("Rodzaj wyników jest wymagany.");
        }

        Section section = resolveSection(entryId, request);
        String content = readFileContent(file);

        if (content.isBlank()) {
            throw new IllegalArgumentException("Przesłany plik wyników nie zawiera żadnej treści.");
        }

        if (content.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Przesłany plik nie jest prawidłowym plikiem tekstowym.");
        }

        FlightResult result = new FlightResult();
        result.setFlightPlanEntry(entry);
        result.setScope(request.scope());
        result.setSection(section);
        result.setOriginalFileName(sanitizeFileName(file.getOriginalFilename()));
        result.setContent(content);
        result.setUploadedBy(uploader);

        return flightResultRepository.save(result).getId();
    }

    @Transactional(readOnly = true)
    public FlightResultFileDto getResultFile(Long resultId) {
        FlightResult result = flightResultRepository.findById(resultId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono wyników lotu o ID: " + resultId
                ));

        return new FlightResultFileDto(
                result.getOriginalFileName(),
                result.getContent()
        );
    }

    @Transactional
    public void deleteResult(Long resultId, String userEmail) {
        requireAdministrator(userEmail);

        FlightResult result = flightResultRepository.findById(resultId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono wyników lotu o ID: " + resultId
                ));

        flightResultRepository.delete(result);
    }

    private Section resolveSection(
            Long entryId,
            FlightResultUploadRequest request
    ) {
        if (request.scope() == FlightResultScope.BRANCH) {
            if (request.sectionId() != null) {
                throw new IllegalArgumentException("Wyniki oddziałowe nie mogą być przypisane do konkretnej sekcji.");
            }

            if (flightResultRepository.existsByFlightPlanEntryIdAndScope(entryId, FlightResultScope.BRANCH)) {
                throw new IllegalArgumentException("Wyniki oddziałowe dla tego lotu zostały już wgrane.");
            }

            return null;
        }

        if (request.sectionId() == null) {
            throw new IllegalArgumentException("Dla wyników sekcyjnych należy wybrać sekcję.");
        }

        Section section = sectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono sekcji o ID: " + request.sectionId()
                ));

        if (flightResultRepository.existsByFlightPlanEntryIdAndScopeAndSectionId(entryId, FlightResultScope.SECTION, section.getId())) {
            throw new IllegalArgumentException("Wyniki tej sekcji dla wybranego lotu zostały już wgrane.");
        }

        return section;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Nie wybrano pliku z wynikami lotu.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Plik jest za duży. Maksymalny rozmiar pliku z wynikami to 5 MB.");
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null || !fileName.toLowerCase().endsWith(".txt")) {
            throw new IllegalArgumentException("Dozwolone są wyłącznie pliki z rozszerzeniem .txt.");
        }
    }

    private String readFileContent(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Nie udało się odczytać przesłanego pliku z wynikami.");
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "wyniki.txt";
        }

        String bare = fileName.replaceAll(".*[/\\\\]", "").trim();

        if (bare.isBlank()) {
            return "wyniki.txt";
        }

        return bare.length() > 255 ? bare.substring(bare.length() - 255) : bare;
    }

    private Breeder requireAdministrator(String userEmail) {
        Breeder user = breederRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono użytkownika."
                ));

        if (user.getRole() != Role.ADMINISTRATOR) {
            throw new IllegalStateException("Brak uprawnień. Wynikami lotów może zarządzać wyłącznie administrator.");
        }

        return user;
    }
}