package com.pzhgp.backend.dto;

import java.time.LocalDateTime;

public record ForumCategoryDto(
        Long id,
        String name,
        String description,
        Integer sortOrder,
        LocalDateTime createdAt,
        Boolean canEdit,
        Boolean canDelete
) {}