package com.pzhgp.backend.dto;

import java.time.LocalDateTime;

public record ForumPostDto(
        Long id,
        String authorName,
        String authorRole,
        String body,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        boolean canEdit,
        boolean canDelete
) {}