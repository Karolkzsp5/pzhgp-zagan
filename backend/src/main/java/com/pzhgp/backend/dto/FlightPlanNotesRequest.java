package com.pzhgp.backend.dto;

import jakarta.validation.constraints.Size;

public record FlightPlanNotesRequest(

        @Size(max = 1000, message = "Uwagi do planu lotów gołębi dorosłych mogą mieć maksymalnie 1000 znaków.")
        String adultNotes,

        @Size(max = 1000, message = "Uwagi do planu lotów gołębi młodych mogą mieć maksymalnie 1000 znaków.")
        String youngNotes

) {
}