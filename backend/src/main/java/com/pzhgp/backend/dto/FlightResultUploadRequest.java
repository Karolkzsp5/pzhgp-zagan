package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.FlightResultScope;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FlightResultUploadRequest(
        @NotNull(message = "Rodzaj wyników jest wymagany.")
        FlightResultScope scope,

        @Positive(message = "ID sekcji musi być liczbą dodatnią.")
        Long sectionId

) {}