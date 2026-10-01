package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.FlightResultScope;
import jakarta.validation.constraints.NotNull;

public record FlightResultUploadRequest(

        @NotNull(message = "Rodzaj wyników jest wymagany.")
        FlightResultScope scope,

        Long sectionId

) {
}