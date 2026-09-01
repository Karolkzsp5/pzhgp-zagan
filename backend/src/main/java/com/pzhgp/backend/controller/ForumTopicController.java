package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.ForumTopicDto;
import com.pzhgp.backend.dto.ForumTopicRequest;
import com.pzhgp.backend.dto.TopicAction;
import com.pzhgp.backend.service.ForumTopicService;
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
public class ForumTopicController {

    private final ForumTopicService topicService;

    @GetMapping("/categories/{categoryId}/topics")
    public ResponseEntity<Page<ForumTopicDto>> getTopicsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(topicService.getTopicsByCategory(categoryId, page, size, authentication.getName()));
    }

    @GetMapping("/topics/{topicId}")
    public ResponseEntity<ForumTopicDto> getTopic(
            @PathVariable Long topicId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(topicService.getTopicAndIncrementViews(topicId, authentication.getName()));
    }

    @PostMapping("/topics")
    public ResponseEntity<Void> createTopic(
            @Valid @RequestBody ForumTopicRequest request,
            Authentication authentication
    ) {
        topicService.createTopic(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/topics/{id}/lock")
    public ResponseEntity<Void> toggleLock(@PathVariable Long id, Authentication authentication) {
        topicService.toggleTopicStatus(id, authentication.getName(), TopicAction.LOCK);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/topics/{id}/pin")
    public ResponseEntity<Void> togglePin(@PathVariable Long id, Authentication authentication) {
        topicService.toggleTopicStatus(id, authentication.getName(), TopicAction.PIN);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/topics/{id}")
    public ResponseEntity<Void> deleteTopic(
            @PathVariable Long id,
            Authentication authentication
    ) {
        topicService.deleteTopic(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}