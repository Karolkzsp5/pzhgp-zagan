package com.pzhgp.backend.exception;

public class AuthRateLimitException extends RuntimeException {

    public AuthRateLimitException(String message) {
        super(message);
    }
}
