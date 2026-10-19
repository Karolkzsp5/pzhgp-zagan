package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.LoginRequest;
import com.pzhgp.backend.dto.RegistrationRequest;
import com.pzhgp.backend.exception.AuthRateLimitException;
import com.pzhgp.backend.service.AuthRateLimiter;
import com.pzhgp.backend.service.BreederService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final BreederService breederService;
    private final AuthRateLimiter authRateLimiter;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegistrationRequest request, HttpServletRequest httpRequest) {
        if (!authRateLimiter.tryAcquireRegistration(httpRequest.getRemoteAddr())) {
            throw new AuthRateLimitException("Zbyt wiele prób rejestracji. Spróbuj ponownie później.");
        }

        breederService.registerNewBreeder(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (!authRateLimiter.tryAcquireLogin(httpRequest.getRemoteAddr())) {
            throw new AuthRateLimitException("Zbyt wiele prób logowania. Spróbuj ponownie później.");
        }

        String token = breederService.login(request);
        return ResponseEntity.ok(token);
    }
}
