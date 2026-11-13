package com.pzhgp.backend.dto;

import java.time.Instant;
import java.util.List;

/**
 * Kopia danych osobowych użytkownika i treści, które dodał w serwisie (prawo dostępu do danych z RODO).
 * Plik nie zawiera hasła ani żadnych danych technicznych sesji.
 */
public record AccountDataExportDto(
        Instant exportedAt,
        AccountDto account,
        List<ForumThreadData> forumThreads,
        List<ForumPostData> forumPosts,
        List<AnnouncementData> announcements,
        List<FlightData> flights
) {
    public record ForumThreadData(String title, String category, Instant createdAt) {}

    public record ForumPostData(String thread, String body, Instant createdAt, Instant editedAt) {}

    public record AnnouncementData(String title, String content, Instant createdAt, Instant updatedAt) {}

    public record FlightData(
            String name,
            String ringNumber,
            String releaseSite,
            String fileName,
            Instant startTime,
            Instant endTime,
            double trackDistanceMeters,
            long durationSeconds,
            Instant uploadedAt
    ) {}
}