package com.pzhgp.backend.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("JWT Service Tests")
class JwtServiceTest {

    private static final String TEST_SECRET = "jwt-test-only-do-not-use-in-production-0123456789abcdef0123456789abcdef";

    @Test
    @DisplayName("Should reject an expired JWT signed with the correct secret")
    void shouldRejectExpiredJwt() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);

        String expiredToken = Jwts.builder()
                .subject("breeder@test.pl")
                .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .expiration(Date.from(Instant.now().minusSeconds(120)))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertFalse(jwtService.isTokenValid(expiredToken));
        assertThrows(ExpiredJwtException.class, () -> jwtService.extractEmail(expiredToken));
    }
}
