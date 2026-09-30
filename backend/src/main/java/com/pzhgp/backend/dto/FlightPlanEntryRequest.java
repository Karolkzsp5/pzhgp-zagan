package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.PigeonAgeGroup;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record FlightPlanEntryRequest(

        @NotNull(message = "Grupa wiekowa gołębi jest wymagana.")
        PigeonAgeGroup pigeonAgeGroup,

        @NotNull(message = "Data planowanego lotu jest wymagana.")
        LocalDate scheduledDate,

        @NotBlank(message = "Miejscowość jest wymagana.")
        @Size(max = 100, message = "Nazwa miejscowości może mieć maksymalnie 100 znaków.")
        String location,

        @NotNull(message = "Dystans jest wymagany.")
        @Min(value = 1, message = "Dystans musi wynosić co najmniej 1 km.")
        @Max(value = 3000, message = "Dystans nie może przekraczać 3000 km.")
        Integer distanceKm,

        @Size(max = 30, message = "Kategoria może mieć maksymalnie 30 znaków.")
        String category,

        @NotBlank(message = "Rodzaj listy jest wymagany.")
        @Size(max = 100, message = "Rodzaj listy może mieć maksymalnie 100 znaków.")
        String listType,

        @NotNull(message = "Kolejność lotu jest wymagana.")
        @Min(value = 1, message = "Kolejność lotu musi wynosić co najmniej 1.")
        Integer sortOrder

) {
}