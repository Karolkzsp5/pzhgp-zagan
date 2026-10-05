package com.pzhgp.backend.dto;

import java.util.List;

public record FlightPlanDetailsDto(
        Long id,
        Integer year,
        List<FlightPlanEntryDto> adultFlights,
        List<FlightPlanEntryDto> youngFlights
) {
}