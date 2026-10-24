package com.pzhgp.backend.support;

import com.pzhgp.backend.config.AuthCookieService;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Authenticates integration-test requests using the same cookie/header
 * combination that the browser sends after obtaining a CSRF token.
 * Missing/invalid CSRF is tested separately in AuthIntegrationTest.
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
