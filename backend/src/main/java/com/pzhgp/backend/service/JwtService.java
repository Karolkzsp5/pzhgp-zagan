package com.pzhgp.backend.service;

import com.pzhgp.backend.entity.Breeder;
import io.jsonwebtoken.Claims;
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
 * Token zawiera adres e-mail (subject) i wersję sesji konta. Rola, status i aktualna wersja sesji
 * są przy każdym żądaniu odczytywane z bazy danych, dzięki czemu zablokowanie konta, zmiana hasła
 * i wylogowanie ze wszystkich urządzeń działają od razu.
 */
@Service
public class JwtService {

    /** Ważność tokenu bez „Zapamiętaj mnie”. Ciasteczko takiej sesji znika też po zamknięciu przeglądarki. */
    private static final Duration SESSION_TOKEN_LIFETIME = Duration.ofHours(24);

    /**
     * Ważność tokenu z „Zapamiętaj mnie”. Wcześniej można go unieważnić zmianą hasła
     * albo opcją „Wyloguj ze wszystkich urządzeń”, które zwiększają wersję sesji konta.
     */
    private static final Duration REMEMBER_ME_TOKEN_LIFETIME = Duration.ofDays(30);
    private static final String SESSION_VERSION_CLAIM = "sessionVersion";

    /** Dane odczytane z poprawnego tokenu. */
    public record TokenData(String email, int sessionVersion) {}

    private final SecretKey signingKey;
    private final JwtParser parser;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser().verifyWith(signingKey).build();
    }

    public Duration getTokenLifetime(boolean rememberMe) {
        return rememberMe ? REMEMBER_ME_TOKEN_LIFETIME : SESSION_TOKEN_LIFETIME;
    }

    /** Token zwykłej sesji, bez „Zapamiętaj mnie”. */
    public String generateToken(Breeder breeder) {
        return generateToken(breeder, false);
    }

    public String generateToken(Breeder breeder, boolean rememberMe) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(breeder.getEmail())
                .claim(SESSION_VERSION_CLAIM, breeder.getSessionVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(getTokenLifetime(rememberMe))))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Adres e-mail i wersja sesji zapisane w tokenie.
     *
     * @throws JwtException gdy token wygasł, jest uszkodzony lub ma nieprawidłowy podpis
     */
    public TokenData readToken(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        // Token wydany przed wprowadzeniem wersji sesji jej nie zawiera, co odpowiada wersji 0.
        Integer sessionVersion = claims.get(SESSION_VERSION_CLAIM, Integer.class);
        return new TokenData(claims.getSubject(), sessionVersion == null ? 0 : sessionVersion);
    }
}