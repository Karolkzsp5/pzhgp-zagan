package com.pzhgp.backend.dto;

import java.time.Instant;

public record ForumThreadDto(
        Long id,
        Long categoryId,
        String title,
        String authorName,
        Integer repliesCount,
        Boolean isLocked,
        Boolean isPinned,
        Instant lastPostAt,
        Integer views,
        Instant createdAt,
        Boolean canEdit,
        Boolean canDelete,
        Boolean canModerate
) {}