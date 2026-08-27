package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.AnnouncementRequestDto;
import com.pzhgp.backend.dto.AnnouncementResponseDto;
import com.pzhgp.backend.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public ResponseEntity<List<AnnouncementResponseDto>> getAllAnnouncements() {
        return ResponseEntity.ok(announcementService.getAllAnnouncements());
    }

    @PostMapping
    public ResponseEntity<Void> createAnnouncement(
            @Valid @RequestBody AnnouncementRequestDto requestDto,
            Authentication authentication
    ) {
        announcementService.createAnnouncement(requestDto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequestDto requestDto,
            Authentication authentication
    ) {
        announcementService.updateAnnouncement(id, requestDto, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(
            @PathVariable Long id,
            Authentication authentication
    ) {
        announcementService.deleteAnnouncement(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}