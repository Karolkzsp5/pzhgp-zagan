package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.ForumThreadDto;
import com.pzhgp.backend.dto.ForumThreadRequest;
import com.pzhgp.backend.dto.ThreadAction;
import com.pzhgp.backend.service.ForumThreadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<Page<ForumThreadDto>> getThreadsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(threadService.getThreadsByCategory(categoryId, page, size, authentication.getName()));
    }

    @GetMapping("/threads/{threadId}")
    public ResponseEntity<ForumThreadDto> getThread(
            @PathVariable Long threadId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(threadService.getThreadAndIncrementViews(threadId, authentication.getName()));
    }

    @PostMapping("/threads")
    public ResponseEntity<Void> createThread(
            @Valid @RequestBody ForumThreadRequest request,
            Authentication authentication
    ) {
        threadService.createThread(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/threads/{id}/lock")
    public ResponseEntity<Void> toggleLock(@PathVariable Long id, Authentication authentication) {
        threadService.toggleThreadStatus(id, authentication.getName(), ThreadAction.LOCK);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/threads/{id}/pin")
    public ResponseEntity<Void> togglePin(@PathVariable Long id, Authentication authentication) {
        threadService.toggleThreadStatus(id, authentication.getName(), ThreadAction.PIN);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/threads/{id}")
    public ResponseEntity<Void> deleteThread(
            @PathVariable Long id,
            Authentication authentication
    ) {
        threadService.deleteThread(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}