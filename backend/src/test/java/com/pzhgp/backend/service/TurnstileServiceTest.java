package com.pzhgp.backend.service;

import com.pzhgp.backend.exception.CaptchaUnavailableException;
import com.pzhgp.backend.exception.CaptchaVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("Turnstile Service Unit Tests")
class TurnstileServiceTest {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final String SECRET_KEY = "test-secret-key";
    private static final String TOKEN = "captcha-token";

    // Zamiast prawdziwego połączenia: sprawdza wysłane żądanie i zwraca przygotowaną odpowiedź Cloudflare.
    private MockRestServiceServer cloudflare;
    private TurnstileService turnstileService;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        cloudflare = MockRestServiceServer.bindTo(restClientBuilder).build();
        turnstileService = new TurnstileService(restClientBuilder, VERIFY_URL, SECRET_KEY);
    }

    private void cloudflareResponds(String json) {
        cloudflare.expect(requestTo(VERIFY_URL)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should send the secret key and the token to Cloudflare as a form")
    void shouldSendSecretKeyAndTokenAsForm() {
        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("secret", SECRET_KEY);
        expectedForm.add("response", TOKEN);

        cloudflare.expect(requestTo(VERIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(expectedForm))
                .andRespond(withSuccess("{\"success\": true}", MediaType.APPLICATION_JSON));

        turnstileService.verifyToken(TOKEN);

        cloudflare.verify();
    }

    @Test
    @DisplayName("Should accept a token confirmed by Cloudflare")
    void shouldAcceptValidToken() {
        cloudflareResponds("""
                {
                  "success": true,
                  "challenge_ts": "2026-11-11T10:00:00.000Z",
                  "hostname": "localhost",
                  "error-codes": [],
                  "action": "",
                  "cdata": ""
                }
                """);

        assertDoesNotThrow(() -> turnstileService.verifyToken(TOKEN));
    }

    @Test
    @DisplayName("Should reject an invalid, expired or already used token")
    void shouldRejectInvalidToken() {
        for (String errorCode : new String[]{"invalid-input-response", "timeout-or-duplicate", "missing-input-response"}) {
            cloudflare.reset();
            cloudflareResponds("{\"success\": false, \"error-codes\": [\"" + errorCode + "\"]}");

            CaptchaVerificationException exception = assertThrows(CaptchaVerificationException.class, () -> turnstileService.verifyToken(TOKEN));
            assertEquals("Weryfikacja CAPTCHA nie powiodła się. Spróbuj ponownie.", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should reject a token when Cloudflare gives no error code")
    void shouldRejectTokenWithoutErrorCodes() {
        cloudflareResponds("{\"success\": false}");

        assertThrows(CaptchaVerificationException.class, () -> turnstileService.verifyToken(TOKEN));
    }

    @Test
    @DisplayName("Should report the service as unavailable when the secret key is rejected")
    void shouldReportUnavailableWhenSecretKeyIsInvalid() {
        for (String errorCode : new String[]{"invalid-input-secret", "missing-input-secret", "bad-request", "internal-error"}) {
            cloudflare.reset();
            cloudflareResponds("{\"success\": false, \"error-codes\": [\"" + errorCode + "\"]}");

            CaptchaUnavailableException exception = assertThrows(CaptchaUnavailableException.class, () -> turnstileService.verifyToken(TOKEN));
            assertEquals("Weryfikacja CAPTCHA jest chwilowo niedostępna. Spróbuj ponownie później.", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should report the service as unavailable when Cloudflare returns a server error")
    void shouldReportUnavailableOnServerError() {
        cloudflare.expect(requestTo(VERIFY_URL)).andRespond(withServerError());

        assertThrows(CaptchaUnavailableException.class, () -> turnstileService.verifyToken(TOKEN));
    }

    @Test
    @DisplayName("Should report the service as unavailable when Cloudflare cannot be reached")
    void shouldReportUnavailableOnConnectionFailure() {
        cloudflare.expect(requestTo(VERIFY_URL)).andRespond(withException(new IOException("Connection timed out")));

        assertThrows(CaptchaUnavailableException.class, () -> turnstileService.verifyToken(TOKEN));
    }

    @Test
    @DisplayName("Should report the service as unavailable when the response is not valid JSON")
    void shouldReportUnavailableOnUnreadableResponse() {
        cloudflare.expect(requestTo(VERIFY_URL)).andRespond(withSuccess("<html>Bad Gateway</html>", MediaType.TEXT_HTML));

        assertThrows(CaptchaUnavailableException.class, () -> turnstileService.verifyToken(TOKEN));
    }
}