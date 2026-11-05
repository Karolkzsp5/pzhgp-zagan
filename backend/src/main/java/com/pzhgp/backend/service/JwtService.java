package com.pzhgp.backend.service;

import com.pzhgp.backend.entity.Breeder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Tworzenie i weryfikacja tokenów JWT przechowywanych w ciasteczku HttpOnly.
 * <p>
 * Token zawiera wyłącznie adres e-mail (subject). Rola i status konta są przy każdym
 * żądaniu odczytywane z bazy danych, dzięki czemu zablokowanie konta działa od razu.
 */
@Service
public class JwtService {

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(24);

    private final SecretKey signingKey;
    private final JwtParser parser;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser().verifyWith(signingKey).build();
    }

    public Duration getTokenLifetime() {
        return TOKEN_LIFETIME;
    }

    public String generateToken(Breeder breeder) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(breeder.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TOKEN_LIFETIME)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Adres e-mail zapisany w tokenie.
     *
     * @throws JwtException gdy token wygasł, jest uszkodzony lub ma nieprawidłowy podpis
     */
    public String extractEmail(String token) {
        return parser.parseSignedClaims(token).getPayload().getSubject();
    }
}