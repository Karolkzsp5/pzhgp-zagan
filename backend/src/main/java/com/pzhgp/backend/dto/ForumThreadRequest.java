package com.pzhgp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ForumThreadRequest(
        @NotNull(message = "ID kategorii nie może być puste.")
        Long categoryId,

        @NotBlank(message = "Tytuł wątku nie może być pusty.")
        @Size(min = 5, max = 150, message = "Tytuł wątku musi mieć od 5 do 150 znaków.")
        String title,

        @NotBlank(message = "Treść pierwszej wiadomości nie może być pusta.")
        String initialPostContent
) {}