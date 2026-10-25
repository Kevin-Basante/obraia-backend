package com.obraia.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextSearchTest {

    @Test
    void normalizeRemovesAccentsCaseAndSpaces() {
        assertEquals("bogota d.c.", TextSearch.normalize("  Bogotá D.C. "));
        assertEquals("nino", TextSearch.normalize("NIÑO"));
    }

    @Test
    void searchWithoutAccentsFindsAccentedText() {
        assertTrue(TextSearch.matches("Arena de río (concreto)", "RIO"));
        assertTrue(TextSearch.matches("Medellín", "medellin"));
    }

    @Test
    void emptyQueryMatchesEverything() {
        assertTrue(TextSearch.matches("Gravilla", null));
        assertTrue(TextSearch.matches("Gravilla", "  "));
    }

    @Test
    void differentTextDoesNotMatch() {
        assertFalse(TextSearch.matches("Gravilla", "cemento"));
    }
}
