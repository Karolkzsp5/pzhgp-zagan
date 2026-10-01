package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.FlightResultScope;

public record FlightResultSummaryDto(
        Long id,
        FlightResultScope scope,
        Long sectionId,
        String sectionName,
        Integer sectionSortOrder,
        String originalFileName
) {
}