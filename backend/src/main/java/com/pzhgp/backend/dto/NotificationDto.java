package com.pzhgp.backend.dto;

import java.time.Instant;

public record NotificationDto(
        Long id,
        String message,
        String link,
        boolean isRead,
        String type,
        Instant createdAt
) {}