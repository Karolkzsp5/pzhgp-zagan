package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.BreederResponseDto;
import com.pzhgp.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
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

    @PutMapping("/approve/{id}")
    public ResponseEntity<String> approveAccount(@PathVariable Long id) {
        adminService.approveAccount(id);
        return ResponseEntity.ok("Konto zostało pomyślnie zaakceptowane.");
    }

    @DeleteMapping("/reject/{id}")
    public ResponseEntity<String> rejectAccount(@PathVariable Long id) {
        adminService.rejectAccount(id);
        return ResponseEntity.ok("Konto zostało odrzucone i usunięte.");
    }

    @PutMapping("/block/{id}")
    public ResponseEntity<String> blockAccount(@PathVariable Long id) {
        adminService.blockAccount(id);
        return ResponseEntity.ok("Konto zostało pomyślnie zablokowane.");
    }

    @PutMapping("/unblock/{id}")
    public ResponseEntity<String> unblockAccount(@PathVariable Long id) {
        adminService.unblockAccount(id);
        return ResponseEntity.ok("Konto zostało pomyślnie odblokowane.");
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<String> changeRole(@PathVariable Long id, @RequestBody Map<String, String> requestBody) {
        String newRole = requestBody.get("role");
        if (newRole == null || newRole.trim().isEmpty()) {
            throw new IllegalArgumentException("Nie podano nowej roli.");
        }
        adminService.changeRole(id, newRole);
        return ResponseEntity.ok("Rola została pomyślnie zmieniona.");
    }
}