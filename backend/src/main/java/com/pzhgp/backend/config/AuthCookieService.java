package com.pzhgp.backend.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieService {

    public static final String COOKIE_NAME = "pzhgp_auth";

    @Value("${auth.cookie.secure:true}")
    private boolean secure;

    public void setAuthenticationCookie(HttpServletResponse response, String token, Duration lifetime, boolean rememberMe) {
        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/");

        if (rememberMe) {
            cookie.maxAge(lifetime);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
    }

    public void clearAuthenticationCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}