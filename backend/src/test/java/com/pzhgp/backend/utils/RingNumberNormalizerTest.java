package com.pzhgp.backend.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RingNumberNormalizerTest {

    @Test
    @DisplayName("Removes separators and converts the ring number to upper case")
    void removesSeparatorsAndUpperCases() {
        assertEquals("PL0208241234", RingNumberNormalizer.normalize("PL-0208-24-1234"));
        assertEquals("PL0208241234", RingNumberNormalizer.normalize("pl 0208 24 1234"));
        assertEquals("PL0208241234", RingNumberNormalizer.normalize("PL/0208/24/1234"));
    }

    @Test
    @DisplayName("Different notations of the same ring number normalize to one value")
    void differentNotationsMatch() {
        String reference = RingNumberNormalizer.normalize("PL-0208-24-1234");

        assertEquals(reference, RingNumberNormalizer.normalize("  pl.0208.24.1234  "));
        assertEquals(reference, RingNumberNormalizer.normalize("PL0208241234"));
    }

    @Test
    @DisplayName("Keeps leading zeros, because they carry the section number")
    void keepsLeadingZeros() {
        assertEquals("PL0208241234", RingNumberNormalizer.normalize("PL-0208-24-1234"));
        assertTrue(RingNumberNormalizer.normalize("PL-0001-24-0007").contains("0001"));
        assertTrue(RingNumberNormalizer.normalize("PL-0001-24-0007").endsWith("0007"));
    }

    @Test
    @DisplayName("Does not force a single Polish ring number format")
    void acceptsForeignFormats() {
        assertEquals("DV012345678", RingNumberNormalizer.normalize("DV 01234 5678"));
        assertEquals("NL20249876543", RingNumberNormalizer.normalize("NL-2024-9876543"));
    }

    @Test
    @DisplayName("Returns an empty text for a missing ring number")
    void handlesNullAndBlank() {
        assertEquals("", RingNumberNormalizer.normalize(null));
        assertEquals("", RingNumberNormalizer.normalize("   "));
        assertEquals("", RingNumberNormalizer.normalize("---"));
    }
}
