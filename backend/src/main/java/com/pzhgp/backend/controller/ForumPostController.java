package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.ForumPostDto;
import com.pzhgp.backend.dto.ForumPostRequest;
import com.pzhgp.backend.service.ForumPostService;
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
public class ForumPostController {

    private final ForumPostService postService;

    @GetMapping("/threads/{threadId}/posts")
    public ResponseEntity<Page<ForumPostDto>> getPostsByThread(
            @PathVariable Long threadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(postService.getPostsByThread(threadId, page, size, authentication.getName()));
    }

    @PostMapping("/threads/{threadId}/posts")
    public ResponseEntity<Void> addPostToThread(
            @PathVariable Long threadId,
            @Valid @RequestBody ForumPostRequest request,
            Authentication authentication
    ) {
        postService.addPost(threadId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/posts/{postId}")
    public ResponseEntity<Void> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody ForumPostRequest request,
            Authentication authentication
    ) {
        postService.updatePost(postId, request, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        postService.deletePost(postId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}