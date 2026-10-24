package com.pzhgp.backend.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AuthRateLimiter {

    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final Duration REGISTRATION_WINDOW = Duration.ofHours(1);

    private static final int MAX_LOGIN_ATTEMPTS = 30;
    private static final int MAX_REGISTRATION_ATTEMPTS = 5;
    private static final int CLEANUP_THRESHOLD = 1_000;

    private final Clock clock;

    private final Map<String, Deque<Instant>> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> registrationAttempts = new ConcurrentHashMap<>();

    public AuthRateLimiter() {
        this(Clock.systemUTC());
    }

    AuthRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquireLogin(String clientAddress) {
        return tryAcquire(loginAttempts, clientAddress, LOGIN_WINDOW, MAX_LOGIN_ATTEMPTS);
    }

    public boolean tryAcquireRegistration(String clientAddress) {
        return tryAcquire(registrationAttempts, clientAddress, REGISTRATION_WINDOW, MAX_REGISTRATION_ATTEMPTS);
    }

    private boolean tryAcquire(Map<String, Deque<Instant>> attempts, String clientAddress, Duration window, int maxAttempts) {
        String key = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress;
        Instant now = clock.instant();

        if (attempts.size() > CLEANUP_THRESHOLD) {
            removeExpiredEntries(attempts, now, window);
        }

        AtomicBoolean accepted = new AtomicBoolean(false);

        attempts.compute(key, (ignored, timestamps) -> {
            Deque<Instant> current = timestamps == null ? new ArrayDeque<>() : timestamps;
            discardExpired(current, now, window);

            if (current.size() < maxAttempts) {
                current.addLast(now);
                accepted.set(true);
            }

            return current;
        });

        return accepted.get();
    }

    private void discardExpired(Deque<Instant> timestamps, Instant now, Duration window) {
        Instant windowStart = now.minus(window);

        while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(windowStart)) {
            timestamps.removeFirst();
        }
    }

    private void removeExpiredEntries(Map<String, Deque<Instant>> attempts, Instant now, Duration window) {
        attempts.forEach((key, ignored) ->
                attempts.computeIfPresent(key, (currentKey, timestamps) -> {
                    discardExpired(timestamps, now, window);
                    return timestamps.isEmpty() ? null : timestamps;
                })
        );
    }

    public void reset() {
        loginAttempts.clear();
        registrationAttempts.clear();
    }
}
