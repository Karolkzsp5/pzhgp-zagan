package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.ForumCategoryDto;
import com.pzhgp.backend.dto.ForumCategoryRequest;
import com.pzhgp.backend.service.ForumCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum/categories")
@RequiredArgsConstructor
public class ForumCategoryController {

    private final ForumCategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<ForumCategoryDto>> getAllCategories(Authentication authentication) {
        return ResponseEntity.ok(categoryService.getAllCategories(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForumCategoryDto> getCategoryById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(categoryService.getCategoryById(id, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<Void> createCategory(
            @Valid @RequestBody ForumCategoryRequest request,
            Authentication authentication
    ) {
        categoryService.createCategory(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody ForumCategoryRequest request,
            Authentication authentication
    ) {
        categoryService.updateCategory(id, request, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id, Authentication authentication) {
        categoryService.deleteCategory(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}