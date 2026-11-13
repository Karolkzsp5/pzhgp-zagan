package com.pzhgp.backend.controller;

import com.pzhgp.backend.config.AuthCookieService;
import com.pzhgp.backend.dto.AccountDataExportDto;
import com.pzhgp.backend.dto.AccountDto;
import com.pzhgp.backend.dto.ContactUpdateRequest;
import com.pzhgp.backend.dto.PasswordChangeRequest;
import com.pzhgp.backend.exception.AuthRateLimitException;
import com.pzhgp.backend.service.AccountService;
import com.pzhgp.backend.service.AuthRateLimiter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Konto zalogowanego użytkownika (strona „Ustawienia”). Każda operacja dotyczy wyłącznie własnego konta. */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AuthRateLimiter authRateLimiter;
    private final AuthCookieService authCookieService;

    @GetMapping
    public ResponseEntity<AccountDto> getAccount(Authentication authentication) {
        return ResponseEntity.ok(accountService.getAccount(authentication.getName()));
    }

    @PutMapping("/contact")
    public ResponseEntity<AccountDto> updateContact(@Valid @RequestBody ContactUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(accountService.updateContact(authentication.getName(), request));
    }

    /** Zmiana hasła kończy wszystkie sesje, więc odpowiedź usuwa też ciasteczko bieżącej sesji. */
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Authentication authentication,
            HttpServletResponse response
    ) {
        if (!authRateLimiter.tryAcquirePasswordChange(authentication.getName())) {
            throw new AuthRateLimitException("Zbyt wiele prób zmiany hasła. Spróbuj ponownie później.");
        }

        accountService.changePassword(authentication.getName(), request);
        authCookieService.clearAuthenticationCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAllDevices(Authentication authentication, HttpServletResponse response) {
        accountService.endAllSessions(authentication.getName());
        authCookieService.clearAuthenticationCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public ResponseEntity<AccountDataExportDto> exportData(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(accountService.exportData(authentication.getName()));
    }
}