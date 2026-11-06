package com.pzhgp.backend.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/** Ograniczenie liczby prób logowania i rejestracji z jednego adresu. */
@Component
public class AuthRateLimiter {

    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final Duration REGISTRATION_WINDOW = Duration.ofHours(1);

    private static final int MAX_LOGIN_ATTEMPTS = 30;
    private static final int MAX_REGISTRATION_ATTEMPTS = 5;

    private final SlidingWindowRateLimiter loginAttempts;
    private final SlidingWindowRateLimiter registrationAttempts;

    public AuthRateLimiter(Clock clock) {
        this.loginAttempts = new SlidingWindowRateLimiter(LOGIN_WINDOW, MAX_LOGIN_ATTEMPTS, clock);
        this.registrationAttempts = new SlidingWindowRateLimiter(REGISTRATION_WINDOW, MAX_REGISTRATION_ATTEMPTS, clock);
    }

    public boolean tryAcquireLogin(String clientAddress) {
        return loginAttempts.tryAcquire(clientAddress);
    }

    public boolean tryAcquireRegistration(String clientAddress) {
        return registrationAttempts.tryAcquire(clientAddress);
    }
}
