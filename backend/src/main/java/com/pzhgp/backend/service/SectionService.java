package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.SectionDto;
import com.pzhgp.backend.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;

    /** Sekcje oddziału w kolejności wyświetlania. */
    @Transactional(readOnly = true)
    public List<SectionDto> getAllSections() {
        return sectionRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(section -> new SectionDto(
                        section.getId(),
                        section.getName(),
                        section.getSortOrder()
                ))
                .collect(Collectors.toList());
    }
}
