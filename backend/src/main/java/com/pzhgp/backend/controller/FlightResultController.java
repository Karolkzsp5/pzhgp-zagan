package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.FlightResultFileDto;
import com.pzhgp.backend.dto.FlightResultUploadRequest;
import com.pzhgp.backend.service.FlightResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/flight-results")
@RequiredArgsConstructor
public class FlightResultController {

    private final FlightResultService flightResultService;

    @PostMapping(value = "/entries/{entryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Long>> uploadResult(
            @PathVariable Long entryId,
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") FlightResultUploadRequest metadata,
            Authentication authentication
    ) {
        Long resultId = flightResultService.uploadResult(entryId, file, metadata, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", resultId));
    }

    @GetMapping("/{resultId}/file")
    public ResponseEntity<String> getResultFile(
            @PathVariable Long resultId
    ) {
        FlightResultFileDto file = flightResultService.getResultFile(resultId);

        ContentDisposition contentDisposition = ContentDisposition.inline()
                .filename(file.originalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(file.content());
    }

    @DeleteMapping("/{resultId}")
    public ResponseEntity<Void> deleteResult(
            @PathVariable Long resultId,
            Authentication authentication
    ) {
        flightResultService.deleteResult(resultId, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}