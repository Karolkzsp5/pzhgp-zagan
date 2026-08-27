package com.pzhgp.backend.dto;

import java.time.LocalDateTime;

public record AnnouncementResponseDto(
        Long id,
        String title,
        String content,
        String authorName,
        String authorEmail,
        String authorRole,
        boolean isPinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}