package com.pzhgp.backend.dto;

import java.time.LocalDateTime;

public record ForumPostDto(
        Long id,
        String authorName,
        String body,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        Boolean canEdit,
        Boolean canDelete
) {}