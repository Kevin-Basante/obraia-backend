package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.obraia.backend.dto.blueprint.ComparisonResponse;
import com.obraia.backend.dto.blueprint.ComparisonRowResponse;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.support.BlueprintFixtures;
import com.obraia.backend.support.ProjectFixtures;

class MeasurementComparatorTest {

    private final Project project = ProjectFixtures.project(UUID.randomUUID(), "Casa Prueba");

    @Test
    void withoutAiEveryRowIsManualOnly() {
        ComparisonResponse comparison = MeasurementComparator.compare(List.of(
                wall(MeasurementSource.MANUAL, 1, "10.00"),
                wall(MeasurementSource.MANUAL, 1, "5.00")));

        assertFalse(comparison.aiAvailable());
        ComparisonRowResponse row = comparison.rows().get(0);
        assertEquals(new BigDecimal("37.50"), row.manualAmount());
        assertNull(row.aiAmount());
        assertNull(row.differencePct());
        assertEquals("manual_only", row.status());
    }

    @Test
    void smallDifferenceIsAMatch() {
        ComparisonRowResponse row = MeasurementComparator.compare(List.of(
                wall(MeasurementSource.MANUAL, 1, "10.00"),
                wall(MeasurementSource.OCR, 1, "10.50"))).rows().get(0);

        assertEquals(new BigDecimal("5.00"), row.differencePct());
        assertEquals("match", row.status());
    }

    @Test
    void bigDifferenceNeedsReview() {
        ComparisonRowResponse row = MeasurementComparator.compare(List.of(
                wall(MeasurementSource.MANUAL, 1, "10.00"),
                wall(MeasurementSource.OCR, 1, "8.00"))).rows().get(0);

        assertEquals(new BigDecimal("-20.00"), row.differencePct());
        assertEquals("review", row.status());
    }

    @Test
    void rowsFollowTheConstructionOrderThenTheFloor() {
        ComparisonResponse comparison = MeasurementComparator.compare(List.of(
                wall(MeasurementSource.OCR, 2, "4.00"),
                wall(MeasurementSource.MANUAL, 1, "4.00"),
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.FOUNDATION, 1, "10.00",
                        "0.40", "0.50", 1)));

        assertTrue(comparison.aiAvailable());
        assertEquals(List.of("foundation-1", "wall-1", "wall-2"), comparison.rows().stream()
                .map(row -> row.elementType() + "-" + row.floorNumber())
                .toList());
        assertEquals("ai_only", comparison.rows().get(2).status());
    }

    private BlueprintMeasurement wall(MeasurementSource source, int floor, String length) {
        return BlueprintFixtures.measurement(project, source, ElementType.WALL, floor, length, null, "2.50", 1);
    }
}
