package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.Role;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(
        @NotNull(message = "Nie podano nowej roli.")
        Role role
) {
}
