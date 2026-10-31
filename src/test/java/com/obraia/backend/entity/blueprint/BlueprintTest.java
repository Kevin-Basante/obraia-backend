package com.obraia.backend.entity.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.support.BlueprintFixtures;
import com.obraia.backend.support.ProjectFixtures;

class BlueprintTest {

    private final Project project = ProjectFixtures.project(UUID.randomUUID(), "Casa Planos");

    @Test
    void aNewBlueprintIsPendingUntilTheAiReadsIt() {
        Blueprint blueprint = BlueprintFixtures.blueprint(project, "planta.png");
        assertEquals(ProcessingStatus.PENDING, blueprint.getProcessingStatus());

        blueprint.markCompleted();

        assertEquals(ProcessingStatus.COMPLETED, blueprint.getProcessingStatus());
    }

    @Test
    void aFailedBlueprintCanBeCompletedLater() {
        Blueprint blueprint = BlueprintFixtures.blueprint(project, "planta.png");

        blueprint.markFailed();
        assertEquals(ProcessingStatus.FAILED, blueprint.getProcessingStatus());
        blueprint.markCompleted();

        assertEquals(ProcessingStatus.COMPLETED, blueprint.getProcessingStatus());
    }

    @Test
    void measurementKeepsItsDiscrepancy() {
        BlueprintMeasurement measurement = BlueprintFixtures.measurement(project, MeasurementSource.OCR,
                ElementType.WALL, 1, "4", null, "2.5", 1);

        measurement.updateDiscrepancy(new BigDecimal("-12.50"));

        assertEquals(new BigDecimal("-12.50"), measurement.getDiscrepancyPct());
    }
}
