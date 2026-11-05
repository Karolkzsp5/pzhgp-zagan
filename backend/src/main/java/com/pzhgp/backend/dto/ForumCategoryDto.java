package com.pzhgp.backend.dto;

import java.time.Instant;

public record ForumCategoryDto(
        Long id,
        String name,
        String description,
        Integer sortOrder,
        Instant createdAt,
        Boolean canEdit,
        Boolean canDelete
) {}