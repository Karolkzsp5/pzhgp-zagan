package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.BoardMemberDto;
import com.pzhgp.backend.dto.BoardMemberRequest;
import com.pzhgp.backend.service.BoardMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardMemberService boardMemberService;

    @GetMapping
    public ResponseEntity<List<BoardMemberDto>> getAllBoardMembers() {
        return ResponseEntity.ok(boardMemberService.getAllBoardMembers());
    }

    @PostMapping
    public ResponseEntity<Void> createBoardMember(@Valid @RequestBody BoardMemberRequest request) {
        boardMemberService.createBoardMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateBoardMember(
            @PathVariable Long id,
            @Valid @RequestBody BoardMemberRequest request
    ) {
        boardMemberService.updateBoardMember(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoardMember(@PathVariable Long id) {
        boardMemberService.deleteBoardMember(id);
        return ResponseEntity.noContent().build();
    }
}