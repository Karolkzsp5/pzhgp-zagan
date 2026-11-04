package com.pzhgp.backend.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

@Component
public class AuthCookieService {

    public static final String COOKIE_NAME = "pzhgp_auth";

    @Value("${auth.cookie.secure:true}")
    private boolean secure;

    public void setAuthenticationCookie(HttpServletResponse response, String token, Duration lifetime, boolean rememberMe) {
        ResponseCookie.ResponseCookieBuilder cookie = authenticationCookie(token);

        if (rememberMe) {
            cookie.maxAge(lifetime);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
    }

    public void clearAuthenticationCookie(HttpServletResponse response) {
        ResponseCookie cookie = authenticationCookie("").maxAge(Duration.ZERO).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** Czy żądanie zawiera ciasteczko uwierzytelniające (niezależnie od jego wartości). */
    public boolean hasAuthenticationCookie(HttpServletRequest request) {
        return WebUtils.getCookie(request, COOKIE_NAME) != null;
    }

    /** Wartość ciasteczka uwierzytelniającego albo {@code null}, gdy żądanie go nie zawiera. */
    public String readToken(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);
        return cookie != null ? cookie.getValue() : null;
    }

    private ResponseCookie.ResponseCookieBuilder authenticationCookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/");
    }
}
