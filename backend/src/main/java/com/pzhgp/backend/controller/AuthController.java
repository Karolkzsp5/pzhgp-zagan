package com.pzhgp.backend.controller;

import com.pzhgp.backend.config.AuthCookieService;
import com.pzhgp.backend.dto.CurrentUserDto;
import com.pzhgp.backend.dto.ForgotPasswordRequest;
import com.pzhgp.backend.dto.LoginRequest;
import com.pzhgp.backend.dto.RegistrationRequest;
import com.pzhgp.backend.dto.ResetPasswordRequest;
import com.pzhgp.backend.exception.AuthRateLimitException;
import com.pzhgp.backend.service.AuthRateLimiter;
import com.pzhgp.backend.service.BreederService;
import com.pzhgp.backend.service.JwtService;
import com.pzhgp.backend.service.PasswordResetService;
import com.pzhgp.backend.service.TurnstileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final BreederService breederService;
    private final AuthRateLimiter authRateLimiter;
    private final AuthCookieService authCookieService;
    private final JwtService jwtService;
    private final TurnstileService turnstileService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegistrationRequest request, HttpServletRequest httpRequest) {
        if (!authRateLimiter.tryAcquireRegistration(httpRequest.getRemoteAddr())) {
            throw new AuthRateLimitException("Zbyt wiele prób rejestracji. Spróbuj ponownie później.");
        }

        // CAPTCHA jest sprawdzana po limicie prób, żeby nie dało się wymuszać dowolnie wielu zapytań serwera do Cloudflare.
        turnstileService.verifyToken(request.captchaToken());

        breederService.registerNewBreeder(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        // Spring Security SPA zapisze surowy token w cookie XSRF-TOKEN.
        // Token z argumentu może być maskowany (BREACH); nie zwracamy go jako JSON.
        csrfToken.getToken();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        if (!authRateLimiter.tryAcquireLogin(httpRequest.getRemoteAddr())) {
            throw new AuthRateLimitException("Zbyt wiele prób logowania. Spróbuj ponownie później.");
        }

        String token = breederService.login(request);
        authCookieService.setAuthenticationCookie(httpResponse, token, jwtService.getTokenLifetime(), request.rememberMe());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        if (!authRateLimiter.tryAcquirePasswordReset(httpRequest.getRemoteAddr())) {
            throw new AuthRateLimitException("Zbyt wiele prób odzyskiwania hasła. Spróbuj ponownie później.");
        }

        turnstileService.verifyToken(request.captchaToken());

        // Odpowiedź jest taka sama dla istniejącego i nieistniejącego konta, żeby nie zdradzać zarejestrowanych adresów.
        passwordResetService.requestPasswordReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        if (!authRateLimiter.tryAcquirePasswordReset(httpRequest.getRemoteAddr())) {
            throw new AuthRateLimitException("Zbyt wiele prób odzyskiwania hasła. Spróbuj ponownie później.");
        }

        passwordResetService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authCookieService.clearAuthenticationCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserDto> currentUser(Authentication authentication) {
        return ResponseEntity.ok(breederService.getCurrentUser(authentication.getName()));
    }
}