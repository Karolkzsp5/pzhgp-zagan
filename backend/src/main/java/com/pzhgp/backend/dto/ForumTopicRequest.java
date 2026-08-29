package com.pzhgp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ForumTopicRequest(
        @NotNull(message = "ID kategorii nie może być puste.")
        Long categoryId,

        @NotBlank(message = "Tytuł tematu nie może być pusty.")
        @Size(max = 255, message = "Tytuł może mieć maksymalnie 255 znaków.")
        String title,

        @NotBlank(message = "Treść pierwszej wiadomości nie może być pusta.")
        String initialPostContent
) {}