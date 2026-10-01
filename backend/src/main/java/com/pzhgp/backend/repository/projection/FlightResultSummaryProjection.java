package com.pzhgp.backend.repository.projection;

import com.pzhgp.backend.entity.FlightResultScope;

public interface FlightResultSummaryProjection {
    Long getId();
    Long getFlightPlanEntryId();
    FlightResultScope getScope();
    Long getSectionId();
    String getSectionName();
    Integer getSectionSortOrder();
    String getOriginalFileName();
}