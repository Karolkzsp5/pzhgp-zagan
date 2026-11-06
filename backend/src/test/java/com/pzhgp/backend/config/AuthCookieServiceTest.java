package com.pzhgp.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Auth Cookie Service Unit Tests")
class AuthCookieServiceTest {

    private AuthCookieService serviceWithSecureFlag(boolean secure) {
        AuthCookieService service = new AuthCookieService();
        ReflectionTestUtils.setField(service, "secure", secure);
        return service;
    }

    @Test
    @DisplayName("Should mark the authentication cookie as Secure when the flag is enabled")
    void shouldSetSecureCookieWhenEnabled() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        serviceWithSecureFlag(true).setAuthenticationCookie(response, "token", Duration.ofHours(1), true);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("HttpOnly"));
    }

    @Test
    @DisplayName("Should not mark the authentication cookie as Secure when the flag is disabled")
    void shouldNotSetSecureCookieWhenDisabled() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        serviceWithSecureFlag(false).setAuthenticationCookie(response, "token", Duration.ofHours(1), true);

        assertFalse(response.getHeader(HttpHeaders.SET_COOKIE).contains("Secure"));
    }

    @Test
    @DisplayName("Should keep the Secure flag when clearing the authentication cookie")
    void shouldKeepSecureFlagWhenClearingCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        serviceWithSecureFlag(true).clearAuthenticationCookie(response);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("Max-Age=0"));
    }
}
