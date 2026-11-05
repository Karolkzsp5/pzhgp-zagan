package com.pzhgp.backend.dto;

import java.time.LocalDateTime;

public record ForumThreadDto(
        Long id,
        Long categoryId,
        String title,
        String authorName,
        Integer repliesCount,
        Boolean isLocked,
        Boolean isPinned,
        LocalDateTime lastPostAt,
        Integer views,
        LocalDateTime createdAt,
        Boolean canEdit,
        Boolean canDelete,
        Boolean canModerate
) {}