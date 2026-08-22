package com.pzhgp.backend.controller;

import com.pzhgp.backend.entity.Section;
import com.pzhgp.backend.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionRepository sectionRepository;

    @GetMapping
    public ResponseEntity<List<Section>> getAllSections() {
        return ResponseEntity.ok(sectionRepository.findAllByOrderBySortOrderAsc());
    }
}