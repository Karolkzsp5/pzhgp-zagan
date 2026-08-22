package com.pzhgp.backend.config;

import com.pzhgp.backend.entity.Section;
import com.pzhgp.backend.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SectionRepository sectionRepository;

    private record SectionSeed(String name, int sortOrder) {}

    @Override
    public void run(String... args) {
        List<SectionSeed> defaultSections = List.of(
                new SectionSeed("Żagań", 1),
                new SectionSeed("Wymiarki", 2),
                new SectionSeed("Chotków", 3),
                new SectionSeed("Kożuchów", 4)
        );

        for (SectionSeed seed : defaultSections) {
            if (!sectionRepository.existsByName(seed.name())) {
                sectionRepository.save(new Section(null, seed.name(), seed.sortOrder()));
                log.info("Zainicjalizowano brakującą sekcję: {} (sortOrder: {})", seed.name(), seed.sortOrder());
            }
        }
    }
}