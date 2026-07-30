package com.pzhgp.backend.dto;

public record LoginRequest(
        String email,
        String password
) {
}