package com.pzhgp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ThreadTitleRequest(
        @NotBlank(message = "Tytuł wątku nie może być pusty.")
        @Size(min = 5, max = 150, message = "Tytuł wątku musi mieć od 5 do 150 znaków.")
        String title
) {

    public ThreadTitleRequest {
        // Długość jest sprawdzana bez spacji na początku i końcu.
        title = title == null ? null : title.trim();
    }
}
