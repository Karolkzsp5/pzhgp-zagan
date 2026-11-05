package com.pzhgp.backend.dto;

import java.time.Instant;

public record ForumPostDto(
        Long id,
        String authorName,
        String authorRole,
        String body,
        Instant createdAt,
        Instant editedAt,
        boolean canEdit,
        boolean canDelete
) {}