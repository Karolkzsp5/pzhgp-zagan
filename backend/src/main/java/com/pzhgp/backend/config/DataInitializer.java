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

    @Override
    public void run(String... args) {
        if (sectionRepository.count() == 0) {
            log.info("Baza danych sekcji jest pusta. Rozpoczynnie inicjalizacji.");

            List<Section> defaultSections = List.of(
                    new Section(null, "Żagań"),
                    new Section(null, "Wymiarki"),
                    new Section(null, "Chotków"),
                    new Section(null, "Kożuchów")
            );

            sectionRepository.saveAll(defaultSections);
            log.info("Pomyślnie zainicjalizowano {} sekcje podstawowe.", defaultSections.size());
        } else {
            log.info("Sekcje są już załadowane w bazie danych. Pomijam inicjalizację.");
        }
    }
}