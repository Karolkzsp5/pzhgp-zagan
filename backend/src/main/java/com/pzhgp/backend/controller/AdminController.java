package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.BreederResponseDto;
import com.pzhgp.backend.dto.RoleChangeRequest;
import com.pzhgp.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/pending")
    public ResponseEntity<List<BreederResponseDto>> getPendingAccounts() {
        return ResponseEntity.ok(adminService.getPendingAccounts());
    }

    @GetMapping("/registered")
    public ResponseEntity<List<BreederResponseDto>> getRegisteredAccounts() {
        return ResponseEntity.ok(adminService.getAllRegisteredAccounts());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approveAccount(@PathVariable Long id, Authentication authentication) {
        adminService.approveAccount(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> rejectAccount(@PathVariable Long id) {
        adminService.rejectAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<Void> blockAccount(@PathVariable Long id) {
        adminService.blockAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/unblock")
    public ResponseEntity<Void> unblockAccount(@PathVariable Long id) {
        adminService.unblockAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<Void> changeRole(@PathVariable Long id, @Valid @RequestBody RoleChangeRequest request, Authentication authentication) {
        adminService.changeRole(id, request.role(), authentication.getName());
        return ResponseEntity.noContent().build();
    }
}