package com.obraia.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.catalog.ElementType;

class BlueprintAnalysisTest {

    private static final AiMeasurement WALL = new AiMeasurement(ElementType.WALL, 1, new BigDecimal("36.00"), null,
            new BigDecimal("2.40"), 2, "Muros de fachada");

    @Test
    void asCachedKeepsTheDataAndMarksIt() {
        BlueprintAnalysis analysis = new BlueprintAnalysis(List.of(WALL), List.of("Escala no visible"), 1,
                "gemini-test", false);

        BlueprintAnalysis cached = analysis.asCached();

        assertFalse(analysis.cached());
        assertTrue(cached.cached());
        assertEquals(analysis.measurements(), cached.measurements());
        assertEquals(analysis.warnings(), cached.warnings());
        assertEquals(1, cached.discarded());
    }

    @Test
    void listsCannotBeChangedFromOutside() {
        List<AiMeasurement> measurements = new ArrayList<>(List.of(WALL));
        BlueprintAnalysis analysis = new BlueprintAnalysis(measurements, List.of(), 0, "gemini-test", false);

        measurements.clear();

        assertEquals(1, analysis.measurements().size());
        assertThrows(UnsupportedOperationException.class, () -> analysis.measurements().clear());
    }
}
