package com.pzhgp.backend.dto;

/**
 * Hodowca, którego można wybrać do zarządu. Zawiera tylko dane potrzebne w formularzu,
 * bez danych kontaktowych, adresu i daty urodzenia.
 */
public record BoardCandidateDto(
        Long id,
        String name,
        String surname,
        Long sectionId
) {}
