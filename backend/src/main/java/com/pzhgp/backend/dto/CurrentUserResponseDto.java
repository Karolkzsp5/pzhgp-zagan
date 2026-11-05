package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Role;

public record CurrentUserResponseDto(
        Long id,
        String email,
        String name,
        Role role,
        AccountStatus status
) {}