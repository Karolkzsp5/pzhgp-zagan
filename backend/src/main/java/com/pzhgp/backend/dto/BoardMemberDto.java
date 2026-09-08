package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.BoardRole;

public record BoardMemberDto(
        Long id,
        BoardRole role,
        Integer sortOrder,
        Long managedSectionId,
        String managedSectionName,
        String firstName,
        String lastName,
        String publicPhoneNumber,
        Long breederId
) {}