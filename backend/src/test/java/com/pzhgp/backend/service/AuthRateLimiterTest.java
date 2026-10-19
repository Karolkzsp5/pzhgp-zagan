package com.pzhgp.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthRateLimiterTest {

    private AuthRateLimiter rateLimiter;
    private AtomicReference<Instant> currentTime;

    @BeforeEach
    void setUp() {
        currentTime = new AtomicReference<>(Instant.parse("2026-10-16T10:00:00Z"));

        Clock clock = mock(Clock.class);
        when(clock.instant()).thenAnswer(invocation -> currentTime.get());

        rateLimiter = new AuthRateLimiter(clock);
    }

    @Test
    @DisplayName("Should reject login attempts after the limit is reached")
    void shouldLimitLoginAttempts() {
        for (int i = 0; i < 30; i++) {
            assertTrue(rateLimiter.tryAcquireLogin("192.0.2.10"));
        }

        assertFalse(rateLimiter.tryAcquireLogin("192.0.2.10"));
        assertTrue(rateLimiter.tryAcquireLogin("192.0.2.11"));
    }

    @Test
    @DisplayName("Should reset login limit after fifteen minutes")
    void shouldResetLoginLimitAfterWindowExpires() {
        for (int i = 0; i < 30; i++) {
            assertTrue(rateLimiter.tryAcquireLogin("192.0.2.10"));
        }

        assertFalse(rateLimiter.tryAcquireLogin("192.0.2.10"));
        currentTime.updateAndGet(time -> time.plus(Duration.ofMinutes(15)));
        assertTrue(rateLimiter.tryAcquireLogin("192.0.2.10"));
    }

    @Test
    @DisplayName("Should reject registration attempts after the limit is reached")
    void shouldLimitRegistrationAttempts() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquireRegistration("192.0.2.10"));
        }

        assertFalse(rateLimiter.tryAcquireRegistration("192.0.2.10"));
        assertTrue(rateLimiter.tryAcquireRegistration("192.0.2.11"));
    }

    @Test
    @DisplayName("Should reset registration limit after one hour")
    void shouldResetRegistrationLimitAfterWindowExpires() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquireRegistration("192.0.2.10"));
        }

        assertFalse(rateLimiter.tryAcquireRegistration("192.0.2.10"));
        currentTime.updateAndGet(time -> time.plus(Duration.ofHours(1)));
        assertTrue(rateLimiter.tryAcquireRegistration("192.0.2.10"));
    }

    @Test
    @DisplayName("Should maintain independent login and registration limits")
    void shouldKeepLoginAndRegistrationLimitsIndependent() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquireRegistration("192.0.2.10"));
        }

        assertFalse(rateLimiter.tryAcquireRegistration("192.0.2.10"));
        assertTrue(rateLimiter.tryAcquireLogin("192.0.2.10"));
    }
}
