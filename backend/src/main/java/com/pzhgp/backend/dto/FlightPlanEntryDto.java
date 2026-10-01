package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.PigeonAgeGroup;

import java.time.LocalDate;
import java.util.List;

public record FlightPlanEntryDto(
        Long id,
        PigeonAgeGroup pigeonAgeGroup,
        LocalDate scheduledDate,
        String location,
        Integer distanceKm,
        String category,
        String listType,
        Integer sortOrder,
        List<FlightResultSummaryDto> results
) {
}