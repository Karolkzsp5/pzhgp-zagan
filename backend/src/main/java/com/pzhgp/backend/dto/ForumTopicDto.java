package com.pzhgp.backend.dto;

import java.time.LocalDateTime;

public record ForumTopicDto(
        Long id,
        Long categoryId,
        String title,
        String authorName,
        Boolean isLocked,
        Boolean isPinned,
        LocalDateTime lastPostAt,
        Integer views,
        LocalDateTime createdAt,
        Boolean canDelete,
        Boolean canModerate
) {}