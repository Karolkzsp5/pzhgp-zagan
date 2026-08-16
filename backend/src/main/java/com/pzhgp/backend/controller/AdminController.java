package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.BreederResponseDto;
import com.pzhgp.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // GET: http://localhost:8080/api/admin/pending
    @GetMapping("/pending")
    public ResponseEntity<List<BreederResponseDto>> getPendingAccounts() {
        return ResponseEntity.ok(adminService.getPendingAccounts());
    }

    // GET: http://localhost:8080/api/admin/registered
    @GetMapping("/registered")
    public ResponseEntity<List<BreederResponseDto>> getRegisteredAccounts() {
        return ResponseEntity.ok(adminService.getAllRegisteredAccounts());
    }

    // PUT: http://localhost:8080/api/admin/approve/{id}
    @PutMapping("/approve/{id}")
    public ResponseEntity<String> approveAccount(@PathVariable Long id) {
        try {
            adminService.approveAccount(id);
            return ResponseEntity.ok("Konto zostało pomyślnie zaakceptowane.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE: http://localhost:8080/api/admin/reject/{id}
    @DeleteMapping("/reject/{id}")
    public ResponseEntity<String> rejectAccount(@PathVariable Long id) {
        try {
            adminService.rejectAccount(id);
            return ResponseEntity.ok("Konto zostało odrzucone i usunięte.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT: http://localhost:8080/api/admin/block/{id}
    @PutMapping("/block/{id}")
    public ResponseEntity<String> blockAccount(@PathVariable Long id) {
        try {
            adminService.blockAccount(id);
            return ResponseEntity.ok("Konto zostało pomyślnie zablokowane.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT: http://localhost:8080/api/admin/unblock/{id}
    @PutMapping("/unblock/{id}")
    public ResponseEntity<String> unblockAccount(@PathVariable Long id) {
        try {
            adminService.unblockAccount(id);
            return ResponseEntity.ok("Konto zostało pomyślnie odblokowane.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT: http://localhost:8080/api/admin/{id}/role
    @PutMapping("/{id}/role")
    public ResponseEntity<String> changeRole(@PathVariable Long id, @RequestBody Map<String, String> requestBody) {
        try {
            String newRole = requestBody.get("role");
            if (newRole == null || newRole.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Nie podano nowej roli.");
            }
            adminService.changeRole(id, newRole);
            return ResponseEntity.ok("Rola została pomyślnie zmieniona.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}