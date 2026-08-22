package com.pzhgp.backend.dto;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String message,
        String link,
        boolean isRead,
        String type,
        LocalDateTime createdAt
) {}