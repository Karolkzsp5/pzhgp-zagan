package com.pzhgp.backend.dto;

import java.time.Instant;

public record AnnouncementResponseDto(
        Long id,
        String title,
        String content,
        String authorName,
        boolean isPinned,
        Instant createdAt,
        Instant updatedAt,
        boolean canEdit,
        boolean canDelete
) {}