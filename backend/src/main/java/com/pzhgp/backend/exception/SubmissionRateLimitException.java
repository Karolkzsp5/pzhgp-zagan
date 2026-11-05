package com.pzhgp.backend.exception;

public class SubmissionRateLimitException extends RuntimeException {

    public SubmissionRateLimitException(String message) {
        super(message);
    }
}