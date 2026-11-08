package com.pzhgp.backend.support;

import com.pzhgp.backend.config.AuthCookieService;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Uwierzytelnia zapytania w testach integracyjnych tym samym zestawem ciasteczek i nagłówków,
 * który przeglądarka wysyła po pobraniu tokenu CSRF. Brak lub błędny token CSRF sprawdza
 * osobno AuthIntegrationTest.
 */
public final class CookieAuthTestSupport {

    private CookieAuthTestSupport() {}

    public static RequestPostProcessor cookieAuth(String jwt) {
        return request -> {
            String csrfToken = "integration-test-csrf-token";
            request.setCookies(
                    new Cookie(AuthCookieService.COOKIE_NAME, jwt),
                    new Cookie("XSRF-TOKEN", csrfToken)
            );
            request.addHeader("X-XSRF-TOKEN", csrfToken);
            return request;
        };
    }
}
