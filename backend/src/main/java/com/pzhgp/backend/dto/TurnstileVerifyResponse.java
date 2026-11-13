package com.pzhgp.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Odpowiedź API Cloudflare Turnstile (siteverify) na sprawdzenie tokenu CAPTCHA.
 * <p>
 * Cloudflare zwraca także m.in. czas rozwiązania zadania i nazwę hosta; aplikacja ich nie używa.
 *
 * @param success    czy token jest prawidłowy
 * @param errorCodes kody błędów nieudanej weryfikacji, np. {@code timeout-or-duplicate}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TurnstileVerifyResponse(
        boolean success,
        @JsonProperty("error-codes") List<String> errorCodes
) {
    public TurnstileVerifyResponse {
        errorCodes = errorCodes == null ? List.of() : List.copyOf(errorCodes);
    }
}