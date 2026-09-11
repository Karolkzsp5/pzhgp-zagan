package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.FlightDetailsDto;
import com.pzhgp.backend.dto.FlightSummaryDto;
import com.pzhgp.backend.dto.FlightUploadRequest;
import com.pzhgp.backend.service.PigeonFlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Moduł "Mapy lotów" — wgrywanie tras GPX i udostępnianie analizy lotu.
 * Wszystkie operacje wymagają zalogowanego, aktywnego konta hodowcy.
 */
@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class PigeonFlightController {

    private final PigeonFlightService pigeonFlightService;

    /**
     * Wgrywa plik GPX z trasą gołębia i zwraca identyfikator zapisanego lotu.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Long>> uploadFlight(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart(value = "metadata", required = false) FlightUploadRequest metadata,
            Authentication authentication
    ) {
        Long flightId = pigeonFlightService.uploadFlight(file, metadata, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", flightId));
    }

    /**
     * Lista lotów zalogowanego hodowcy.
     */
    @GetMapping
    public ResponseEntity<Page<FlightSummaryDto>> getMyFlights(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(pigeonFlightService.getMyFlights(authentication.getName(), page, size));
    }

    /**
     * Szczegóły lotu wraz z trasą gotową do wyświetlenia na mapie.
     *
     * @param tolerance tolerancja upraszczania trasy w metrach; 0 zwraca pełną trasę
     */
    @GetMapping("/{id}")
    public ResponseEntity<FlightDetailsDto> getFlight(
            @PathVariable Long id,
            @RequestParam(required = false) Double tolerance,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                pigeonFlightService.getFlightDetails(id, authentication.getName(), tolerance));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlight(@PathVariable Long id, Authentication authentication) {
        pigeonFlightService.deleteFlight(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
