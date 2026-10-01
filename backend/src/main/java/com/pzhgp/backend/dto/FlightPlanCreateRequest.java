package com.pzhgp.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FlightPlanCreateRequest(

        @NotNull(message = "Rok planu lotu jest wymagany.")
        @Min(value = 2000, message = "Rok planu nie może być wcześniejszy niż 2000.")
        @Max(value = 2100, message = "Rok planu nie może być późniejszy niż 2100.")
        Integer year

) {
}