package com.pzhgp.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ForumPostRequest(
        @NotBlank(message = "Treść wpisu nie może być pusta.")
        String body
) {}