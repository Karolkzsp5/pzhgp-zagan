package com.pzhgp.backend.exception;

/** Token CAPTCHA został odrzucony: jest nieprawidłowy, już wykorzystany albo przeterminowany. */
public class CaptchaVerificationException extends RuntimeException {

    public CaptchaVerificationException(String message) {
        super(message);
    }
}