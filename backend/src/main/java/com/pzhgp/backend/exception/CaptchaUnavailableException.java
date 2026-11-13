package com.pzhgp.backend.exception;

/** Tokenu CAPTCHA nie udało się sprawdzić, bo usługa Cloudflare Turnstile nie odpowiedziała poprawnie. */
public class CaptchaUnavailableException extends RuntimeException {

    public CaptchaUnavailableException(String message) {
        super(message);
    }
}