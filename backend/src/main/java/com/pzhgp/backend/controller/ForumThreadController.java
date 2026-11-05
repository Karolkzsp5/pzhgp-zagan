package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.ForumThreadDto;
import com.pzhgp.backend.dto.ForumThreadRequest;
import com.pzhgp.backend.dto.PageResponse;
import com.pzhgp.backend.dto.ThreadStatusRequest;
import com.pzhgp.backend.dto.ThreadTitleRequest;
import com.pzhgp.backend.service.ForumThreadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumThreadController {

    private final ForumThreadService threadService;

    @GetMapping("/categories/{categoryId}/threads")
    public ResponseEntity<PageResponse<ForumThreadDto>> getThreadsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(PageResponse.from(threadService.getThreadsByCategory(categoryId, page, size, authentication.getName())));
    }

    @GetMapping("/threads/{threadId}")
    public ResponseEntity<ForumThreadDto> getThread(
            @PathVariable Long threadId,
            @RequestParam(defaultValue = "true") boolean countView,
            Authentication authentication
    ) {
        return ResponseEntity.ok(threadService.getThread(threadId, authentication.getName(), countView));
    }

    @PostMapping("/threads")
    public ResponseEntity<Void> createThread(
            @Valid @RequestBody ForumThreadRequest request,
            Authentication authentication
    ) {
        threadService.createThread(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/threads/{id}/title")
    public ResponseEntity<Void> updateThreadTitle(
            @PathVariable Long id,
            @Valid @RequestBody ThreadTitleRequest request,
            Authentication authentication) {
        threadService.updateThreadTitle(id, request.title(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/threads/{id}")
    public ResponseEntity<Void> deleteThread(
            @PathVariable Long id,
            Authentication authentication
    ) {
        threadService.deleteThread(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/threads/{id}/status")
    public ResponseEntity<Void> updateThreadStatus(
            @PathVariable Long id,
            @Valid @RequestBody ThreadStatusRequest request,
            Authentication authentication) {
        threadService.updateThreadStatus(id, request, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}