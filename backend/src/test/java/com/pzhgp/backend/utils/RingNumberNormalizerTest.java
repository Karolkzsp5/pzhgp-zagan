package com.pzhgp.backend.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RingNumberNormalizerTest {

    @Test
    @DisplayName("Removes separators and converts the ring number to upper case")
    void removesSeparatorsAndUpperCases() {
        assertEquals("PL0369261234", RingNumberNormalizer.normalize("PL-0369-26-1234"));
        assertEquals("PL0369261234", RingNumberNormalizer.normalize("pl 0369 26 1234"));
        assertEquals("PL0369261234", RingNumberNormalizer.normalize("PL/0369/26/1234"));
    }

    @Test
    @DisplayName("Different notations of the same ring number normalize to one value")
    void differentNotationsMatch() {
        String reference = RingNumberNormalizer.normalize("PL-0369-26-1234");

        assertEquals(reference, RingNumberNormalizer.normalize("  pl.0369.26.1234  "));
        assertEquals(reference, RingNumberNormalizer.normalize("PL0369261234"));
    }

    @Test
    @DisplayName("Keeps leading zeros, because they carry the section number")
    void keepsLeadingZeros() {
        assertEquals("PL0369261234", RingNumberNormalizer.normalize("PL-0369-26-1234"));
        assertTrue(RingNumberNormalizer.normalize("PL-0001-24-0007").contains("0001"));
        assertTrue(RingNumberNormalizer.normalize("PL-0001-24-0007").endsWith("0007"));
    }

    @Test
    @DisplayName("Normalizes partial ring number search phrases")
    void normalizesPartialSearchPhrases() {
        assertEquals("PL0369", RingNumberNormalizer.normalize("pl-0369"));
        assertEquals("261234", RingNumberNormalizer.normalize("26 1234"));
    }

    @Test
    @DisplayName("Returns an empty text for a missing ring number")
    void handlesNullAndBlank() {
        assertEquals("", RingNumberNormalizer.normalize(null));
        assertEquals("", RingNumberNormalizer.normalize("   "));
        assertEquals("", RingNumberNormalizer.normalize("---"));
    }
}
