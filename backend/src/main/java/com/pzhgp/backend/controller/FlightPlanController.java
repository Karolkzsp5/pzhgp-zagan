package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.FlightPlanCreateRequest;
import com.pzhgp.backend.dto.FlightPlanDetailsDto;
import com.pzhgp.backend.dto.FlightPlanEntryRequest;
import com.pzhgp.backend.dto.FlightPlanNotesRequest;
import com.pzhgp.backend.dto.FlightPlanSummaryDto;
import com.pzhgp.backend.service.FlightPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flight-plans")
@RequiredArgsConstructor
public class FlightPlanController {

    private final FlightPlanService flightPlanService;

    @GetMapping
    public ResponseEntity<List<FlightPlanSummaryDto>> getAllPlans() {
        return ResponseEntity.ok(flightPlanService.getAllPlans());
    }

    @GetMapping("/{year}")
    public ResponseEntity<FlightPlanDetailsDto> getPlanByYear(@PathVariable Integer year) {
        return ResponseEntity.ok(flightPlanService.getPlanByYear(year));
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> createPlan(
            @Valid @RequestBody FlightPlanCreateRequest request,
            Authentication authentication
    ) {
        Long planId = flightPlanService.createPlan(request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", planId));
    }

    @PutMapping("/{year}/notes")
    public ResponseEntity<Void> updateNotes(
            @PathVariable Integer year,
            @Valid @RequestBody FlightPlanNotesRequest request,
            Authentication authentication
    ) {
        flightPlanService.updateNotes(year, request, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{year}/entries")
    public ResponseEntity<Map<String, Long>> addEntry(
            @PathVariable Integer year,
            @Valid @RequestBody FlightPlanEntryRequest request,
            Authentication authentication
    ) {
        Long entryId = flightPlanService.addEntry(year, request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", entryId));
    }

    @PutMapping("/entries/{entryId}")
    public ResponseEntity<Void> updateEntry(
            @PathVariable Long entryId,
            @Valid @RequestBody FlightPlanEntryRequest request,
            Authentication authentication
    ) {
        flightPlanService.updateEntry(entryId, request, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/entries/{entryId}")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable Long entryId,
            Authentication authentication
    ) {
        flightPlanService.deleteEntry(entryId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{year}")
    public ResponseEntity<Void> deletePlan(
            @PathVariable Integer year,
            Authentication authentication
    ) {
        flightPlanService.deletePlan(year, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}