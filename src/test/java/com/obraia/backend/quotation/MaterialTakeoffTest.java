package com.obraia.backend.quotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.catalog.Material;
import com.obraia.backend.entity.catalog.MaterialCategory;
import com.obraia.backend.entity.catalog.MaterialFormula;
import com.obraia.backend.entity.catalog.ProjectType;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.support.BlueprintFixtures;
import com.obraia.backend.support.CatalogFixtures;
import com.obraia.backend.support.ProjectFixtures;

class MaterialTakeoffTest {

    private final Project project = ProjectFixtures.project(UUID.randomUUID(), "Casa Cotizada");
    private final ProjectType type = project.getProjectType();
    private final Material block = CatalogFixtures.material("Bloque de arcilla No. 5", MaterialCategory.MASONRY,
            "1600");
    private final Material cement = CatalogFixtures.material("Cemento gris", MaterialCategory.CEMENT_CONCRETE,
            "33000");
    private final List<MaterialFormula> formulas = List.of(
            CatalogFixtures.formula(type, cement, ElementType.WALL, "0.15"),
            CatalogFixtures.formula(type, block, ElementType.WALL, "12.5"),
            CatalogFixtures.formula(type, cement, ElementType.FOUNDATION, "7"));

    @Test
    void wallAreaTimesConsumptionGivesTheQuantity() {
        // 2 walls of 5 m by 2.5 m = 25 m2.
        MaterialTakeoff takeoff = MaterialTakeoff.of(List.of(
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.WALL, 1, "5", null,
                        "2.5", 2)), formulas);

        MaterialNeed blocks = takeoff.needs().get(0);
        assertEquals("Bloque de arcilla No. 5", blocks.material().getName());
        assertEquals(new BigDecimal("312.50"), blocks.quantity());
        // 5 % of waste, rounded to two decimals.
        assertEquals(new BigDecimal("15.63"), blocks.wasteQuantity());
        assertEquals(new BigDecimal("3.75"), takeoff.needs().get(1).quantity());
    }

    @Test
    void aMaterialUsedByTwoElementsIsAddedUp() {
        MaterialTakeoff takeoff = MaterialTakeoff.of(List.of(
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.WALL, 1, "10", null,
                        "2", 1),
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.FOUNDATION, 1, "10",
                        "0.4", "0.5", 1)), formulas);

        assertEquals(2, takeoff.needs().size());
        // 20 m2 x 0.15 + 2 m3 x 7 bags.
        assertEquals(new BigDecimal("17.00"), takeoff.needs().get(1).quantity());
    }

    @Test
    void measurementsOfTheSameElementAreAddedBeforeMultiplying() {
        MaterialTakeoff takeoff = MaterialTakeoff.of(List.of(
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.WALL, 1, "4", null,
                        "2", 1),
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.WALL, 2, "6", null,
                        "2", 1)), formulas);

        assertEquals(new BigDecimal("250.00"), takeoff.needs().get(0).quantity());
    }

    @Test
    void elementsWithoutFormulasAreReported() {
        MaterialTakeoff takeoff = MaterialTakeoff.of(List.of(
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.ROOF, 1, "8", "6",
                        null, 1),
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.WALL, 1, "4", null,
                        "2", 1)), formulas);

        assertEquals(List.of(ElementType.ROOF), takeoff.elementsWithoutFormulas());
        assertEquals(2, takeoff.needs().size());
    }

    @Test
    void noMeasurementsGiveNoMaterials() {
        MaterialTakeoff takeoff = MaterialTakeoff.of(List.of(), formulas);

        assertTrue(takeoff.needs().isEmpty());
        assertTrue(takeoff.elementsWithoutFormulas().isEmpty());
    }
}
