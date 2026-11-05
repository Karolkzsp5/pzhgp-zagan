package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.SectionDto;
import com.pzhgp.backend.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionRepository sectionRepository;

    @GetMapping
    public ResponseEntity<List<SectionDto>> getAllSections() {
        List<SectionDto> sections = sectionRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(section -> new SectionDto(
                        section.getId(),
                        section.getName(),
                        section.getSortOrder()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(sections);
    }
}