package com.obraia.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.support.BlueprintFixtures;
import com.obraia.backend.support.ProjectFixtures;

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

    @Test
    void aiMeasurementBecomesAnOcrMeasurementOfTheBlueprint() {
        Project project = ProjectFixtures.project(UUID.randomUUID(), "Casa IA");
        var blueprint = BlueprintFixtures.blueprint(project, "plano.png");

        BlueprintMeasurement measurement = WALL.toEntity(project, blueprint);

        assertEquals(MeasurementSource.OCR, measurement.getSource());
        assertEquals(blueprint, measurement.getBlueprint());
        assertEquals(ElementType.WALL, measurement.getElementType());
        assertEquals(2, measurement.getQuantity());
        // 36 m by 2.4 m, twice.
        assertEquals(0, new BigDecimal("172.80").compareTo(measurement.getAmount()));
        assertEquals("Muros de fachada", measurement.getNotes());
    }
}
