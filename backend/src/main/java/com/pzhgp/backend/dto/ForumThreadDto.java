package com.pzhgp.backend.dto;

import java.time.Instant;

public record ForumThreadDto(
        Long id,
        Long categoryId,
        String title,
        String authorName,
        Integer repliesCount,
        boolean isLocked,
        boolean isPinned,
        Instant lastPostAt,
        Integer views,
        Instant createdAt,
        boolean canEdit,
        boolean canDelete,
        boolean canModerate
) {}