package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.BreederResponseDto;
import com.pzhgp.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*") // Na czas testów
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // GET: http://localhost:8080/api/admin/pending
    @GetMapping("/pending")
    public ResponseEntity<List<BreederResponseDto>> getPendingAccounts() {
        return ResponseEntity.ok(adminService.getPendingAccounts());
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
}