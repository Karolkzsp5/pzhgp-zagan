package com.pzhgp.backend.service.gpx;

/**
 * Błąd odczytu pliku GPX zgłaszany użytkownikowi (mapowany na HTTP 400).
 */
public class GpxParsingException extends RuntimeException {

    public GpxParsingException(String message) {
        super(message);
    }

    public GpxParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
