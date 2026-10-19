package com.pzhgp.backend.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RingNumberNormalizerTest {

    @Test
    @DisplayName("Should remove separators and convert ring numbers to uppercase")
    void removesSeparatorsAndUpperCases() {
        assertEquals("PL0369261234", RingNumberNormalizer.normalize("PL-0369-26-1234"));
        assertEquals("PL0369261234", RingNumberNormalizer.normalize("pl 0369 26 1234"));
        assertEquals("PL0369261234", RingNumberNormalizer.normalize("PL/0369/26/1234"));
    }

    @Test
    @DisplayName("Should normalize different notations of the same ring number to one value")
    void differentNotationsMatch() {
        String reference = RingNumberNormalizer.normalize("PL-0369-26-1234");

        assertEquals(reference, RingNumberNormalizer.normalize("  pl.0369.26.1234  "));
        assertEquals(reference, RingNumberNormalizer.normalize("PL0369261234"));
    }

    @Test
    @DisplayName("Should preserve leading zeros that encode the section number")
    void keepsLeadingZeros() {
        assertEquals("PL0369261234", RingNumberNormalizer.normalize("PL-0369-26-1234"));
        assertTrue(RingNumberNormalizer.normalize("PL-0001-24-0007").contains("0001"));
        assertTrue(RingNumberNormalizer.normalize("PL-0001-24-0007").endsWith("0007"));
    }

    @Test
    @DisplayName("Should normalize partial ring number search phrases")
    void normalizesPartialSearchPhrases() {
        assertEquals("PL0369", RingNumberNormalizer.normalize("pl-0369"));
        assertEquals("261234", RingNumberNormalizer.normalize("26 1234"));
    }

    @Test
    @DisplayName("Should return an empty string for null or blank ring numbers")
    void handlesNullAndBlank() {
        assertEquals("", RingNumberNormalizer.normalize(null));
        assertEquals("", RingNumberNormalizer.normalize("   "));
        assertEquals("", RingNumberNormalizer.normalize("---"));
    }
}
