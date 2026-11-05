package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.Role;

import java.time.Instant;

public record ForumPostDto(
        Long id,
        String authorName,
        Role authorRole,
        String body,
        Instant createdAt,
        Instant editedAt,
        boolean canEdit,
        boolean canDelete
) {}