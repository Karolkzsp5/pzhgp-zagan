package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.FoundPigeonDto;
import com.pzhgp.backend.dto.FoundPigeonNoteRequest;
import com.pzhgp.backend.dto.FoundPigeonStatusRequest;
import com.pzhgp.backend.entity.FoundPigeonStatus;
import com.pzhgp.backend.service.FoundPigeonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Panel obsługi zgłoszeń odnalezienia gołębi.
 * <p>
 * Cała ścieżka {@code /api/admin/**} wymaga uprawnień administratora — reguła jest
 * zdefiniowana w {@code SecurityConfig}, a nie tylko ukryta w interfejsie.
 */
@RestController
@RequestMapping("/api/admin/found-pigeons")
@RequiredArgsConstructor
public class AdminFoundPigeonController {

    private final FoundPigeonService foundPigeonService;

    /**
     * Lista zgłoszeń od najnowszego, z opcjonalnym filtrem statusu i numeru obrączki.
     */
    @GetMapping
    public ResponseEntity<Page<FoundPigeonDto>> getReports(
            @RequestParam(required = false) FoundPigeonStatus status,
            @RequestParam(required = false) String ringNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(foundPigeonService.searchReports(status, ringNumber, page, size));
    }

    /** Liczba zgłoszeń oczekujących na weryfikację. */
    @GetMapping("/pending-count")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        return ResponseEntity.ok(Map.of("count", foundPigeonService.countPendingReports()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoundPigeonDto> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(foundPigeonService.getReport(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FoundPigeonDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody FoundPigeonStatusRequest request
    ) {
        return ResponseEntity.ok(foundPigeonService.updateStatus(id, request.status()));
    }

    @PatchMapping("/{id}/note")
    public ResponseEntity<FoundPigeonDto> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody FoundPigeonNoteRequest request
    ) {
        return ResponseEntity.ok(foundPigeonService.updateAdminNote(id, request.adminNote()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        foundPigeonService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}
