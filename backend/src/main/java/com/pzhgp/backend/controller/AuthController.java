package com.pzhgp.backend.controller;

import com.pzhgp.backend.config.AuthCookieService;
import com.pzhgp.backend.dto.CurrentUserResponseDto;
import com.pzhgp.backend.dto.LoginRequest;
import com.pzhgp.backend.dto.RegistrationRequest;
import com.pzhgp.backend.exception.AuthRateLimitException;
import com.pzhgp.backend.service.AuthRateLimiter;
import com.pzhgp.backend.service.BreederService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.pzhgp.backend.service.JwtService;
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

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegistrationRequest request, HttpServletRequest httpRequest) {
        if (!authRateLimiter.tryAcquireRegistration(httpRequest.getRemoteAddr())) {
            throw new AuthRateLimitException("Zbyt wiele prób rejestracji. Spróbuj ponownie później.");
        }

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

    @PostMapping("/login-cookie")
    public ResponseEntity<Void> loginWithCookie(
            @Valid @RequestBody LoginRequest request,
            @RequestParam(defaultValue = "false") boolean rememberMe,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        if (!authRateLimiter.tryAcquireLogin(httpRequest.getRemoteAddr())) {
            throw new AuthRateLimitException("Zbyt wiele prób logowania. Spróbuj ponownie później.");
        }

        String token = breederService.login(request);
        authCookieService.setAuthenticationCookie(httpResponse, token, jwtService.getTokenLifetime(), rememberMe);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authCookieService.clearAuthenticationCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponseDto> currentUser(Authentication authentication) {
        return ResponseEntity.ok(breederService.getCurrentUser(authentication.getName()));
    }

}