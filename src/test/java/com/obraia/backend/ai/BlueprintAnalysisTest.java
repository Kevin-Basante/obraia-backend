package com.obraia.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.catalog.ElementType;

class BlueprintAnalysisTest {

    private static final AiMeasurement WALL = new AiMeasurement(ElementType.WALL, 1, new BigDecimal("36.00"), null,
            new BigDecimal("2.40"), 2, "Muros de fachada");

    @Test
    void listsCannotBeChangedFromOutside() {
        List<AiMeasurement> measurements = new ArrayList<>(List.of(WALL));
        BlueprintAnalysis analysis = new BlueprintAnalysis(measurements, List.of(), 0, "gemini-test", false);

        measurements.clear();

        assertEquals(1, analysis.measurements().size());
        assertThrows(UnsupportedOperationException.class, () -> analysis.measurements().clear());
    }
}
