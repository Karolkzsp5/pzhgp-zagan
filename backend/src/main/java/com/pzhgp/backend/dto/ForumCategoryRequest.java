package com.pzhgp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForumCategoryRequest(
        @NotBlank(message = "Nazwa kategorii nie może być pusta.")
        @Size(max = 100, message = "Nazwa kategorii może mieć maksymalnie 100 znaków.")
        String name,
        String description,
        Integer sortOrder
) {}