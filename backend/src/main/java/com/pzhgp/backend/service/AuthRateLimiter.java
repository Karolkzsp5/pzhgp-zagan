package com.pzhgp.backend.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/** Ograniczenie liczby prób logowania, rejestracji i odzyskiwania hasła z jednego adresu oraz prób zmiany hasła na koncie. */
@Component
public class AuthRateLimiter {

    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final Duration REGISTRATION_WINDOW = Duration.ofHours(1);
    private static final Duration PASSWORD_RESET_WINDOW = Duration.ofHours(1);
    private static final Duration PASSWORD_CHANGE_WINDOW = Duration.ofMinutes(15);

    private static final int MAX_LOGIN_ATTEMPTS = 30;
    private static final int MAX_REGISTRATION_ATTEMPTS = 5;
    private static final int MAX_PASSWORD_RESET_ATTEMPTS = 5;
    private static final int MAX_PASSWORD_CHANGE_ATTEMPTS = 5;

    private final SlidingWindowRateLimiter loginAttempts;
    private final SlidingWindowRateLimiter registrationAttempts;
    private final SlidingWindowRateLimiter passwordResetAttempts;
    private final SlidingWindowRateLimiter passwordChangeAttempts;

    public AuthRateLimiter(Clock clock) {
        this.loginAttempts = new SlidingWindowRateLimiter(LOGIN_WINDOW, MAX_LOGIN_ATTEMPTS, clock);
        this.registrationAttempts = new SlidingWindowRateLimiter(REGISTRATION_WINDOW, MAX_REGISTRATION_ATTEMPTS, clock);
        this.passwordResetAttempts = new SlidingWindowRateLimiter(PASSWORD_RESET_WINDOW, MAX_PASSWORD_RESET_ATTEMPTS, clock);
        this.passwordChangeAttempts = new SlidingWindowRateLimiter(PASSWORD_CHANGE_WINDOW, MAX_PASSWORD_CHANGE_ATTEMPTS, clock);
    }

    public boolean tryAcquireLogin(String clientAddress) {
        return loginAttempts.tryAcquire(clientAddress);
    }

    public boolean tryAcquireRegistration(String clientAddress) {
        return registrationAttempts.tryAcquire(clientAddress);
    }

    /** Wspólny limit dla prośby o link i dla ustawienia nowego hasła. */
    public boolean tryAcquirePasswordReset(String clientAddress) {
        return passwordResetAttempts.tryAcquire(clientAddress);
    }

    /**
     * Limit prób zmiany hasła liczony dla konta, a nie adresu: chroni przed odgadywaniem obecnego hasła
     * przez kogoś, kto przejął zalogowaną sesję.
     */
    public boolean tryAcquirePasswordChange(String accountEmail) {
        return passwordChangeAttempts.tryAcquire(accountEmail);
    }
}
