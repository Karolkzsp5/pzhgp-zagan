package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.RegistrationRequest;
import com.pzhgp.backend.service.BreederService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final BreederService breederService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegistrationRequest request) {
        try {
            breederService.registerNewBreeder(request);
            return ResponseEntity.status(201).body("Rejestracja przebiegła pomyślnie. Konto oczekuje na akceptację administratora.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}