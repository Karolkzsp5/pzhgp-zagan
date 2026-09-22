package com.pzhgp.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ForumCategoryRequest(
        @NotBlank(message = "Nazwa kategorii nie może być pusta.")
        @Size(min = 3, max = 100, message = "Nazwa kategorii musi mieć od 3 do 100 znaków.")
        String name,

        String description,

        @NotNull(message = "Kolejność wyświetlania jest wymagana.")
        @Min(value = 1, message = "Kolejność wyświetlania musi wynosić co najmniej 1.")
        Integer sortOrder
) {}