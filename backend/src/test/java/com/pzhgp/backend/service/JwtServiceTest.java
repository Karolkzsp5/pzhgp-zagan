package com.pzhgp.backend.service;

import com.pzhgp.backend.entity.Breeder;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("JWT Service Unit Tests")
class JwtServiceTest {

    private static final String TEST_SECRET = "jwt-test-only-do-not-use-in-production-0123456789abcdef0123456789abcdef";

    @Test
    @DisplayName("Should reject an expired JWT signed with the correct secret")
    void shouldRejectExpiredJwt() {
        JwtService jwtService = new JwtService(TEST_SECRET);

        String expiredToken = Jwts.builder()
                .subject("breeder@test.pl")
                .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .expiration(Date.from(Instant.now().minusSeconds(120)))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThrows(ExpiredJwtException.class, () -> jwtService.readToken(expiredToken));
    }

    @Test
    @DisplayName("Should store the e-mail address and the session version in the token")
    void shouldStoreEmailAndSessionVersion() {
        JwtService jwtService = new JwtService(TEST_SECRET);
        Breeder breeder = new Breeder();
        breeder.setEmail("breeder@test.pl");
        breeder.endAllSessions();
        breeder.endAllSessions();

        JwtService.TokenData tokenData = jwtService.readToken(jwtService.generateToken(breeder));

        assertEquals("breeder@test.pl", tokenData.email());
        assertEquals(2, tokenData.sessionVersion());
    }

    @Test
    @DisplayName("Should treat a token without the session version as version 0")
    void shouldReadTokenWithoutSessionVersion() {
        JwtService jwtService = new JwtService(TEST_SECRET);

        String oldToken = Jwts.builder()
                .subject("breeder@test.pl")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertEquals(0, jwtService.readToken(oldToken).sessionVersion());
    }
}