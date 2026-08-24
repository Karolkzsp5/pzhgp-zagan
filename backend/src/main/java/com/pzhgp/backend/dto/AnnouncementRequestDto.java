package com.pzhgp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementRequestDto(
        @NotBlank(message = "Tytuł ogłoszenia nie może być pusty.")
        @Size(min = 3, max = 150, message = "Tytuł ogłoszenia musi zawierać od 3 do 150 znaków.")
        String title,

        @NotBlank(message = "Treść ogłoszenia nie może być pusta.")
        String content,

        boolean isPinned
) {}