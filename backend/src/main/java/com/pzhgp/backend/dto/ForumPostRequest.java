package com.pzhgp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForumPostRequest(
        @NotBlank(message = "Treść wpisu nie może być pusta.")
        @Size(max = 20_000, message = "Treść wpisu może mieć maksymalnie 20000 znaków.") String body
) {}