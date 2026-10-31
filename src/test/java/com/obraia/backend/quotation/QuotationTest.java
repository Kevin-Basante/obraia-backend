package com.obraia.backend.quotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.catalog.MaterialCategory;
import com.obraia.backend.support.CatalogFixtures;

class QuotationTest {

    private final List<MaterialNeed> needs = List.of(new MaterialNeed(
            CatalogFixtures.material("Gravilla", MaterialCategory.AGGREGATES, "115000"), new BigDecimal("2.00"),
            new BigDecimal("0.10")));

    @Test
    void builderPutsThePartsTogether() {
        Quotation quotation = Quotation.builder().projectId(UUID.randomUUID())
                .location("Pasto", new BigDecimal("6.00"))
                .workingDays(26, Quotation.DaysSource.STANDARD_PLAN)
                .takeoff(new MaterialTakeoff(needs, List.of()))
                .cost(new MaterialsCost(needs))
                .build();

        assertEquals("Pasto", quotation.getCity());
        assertEquals(26, quotation.getWorkingDays());
        assertEquals(new BigDecimal("230000.00"), quotation.total());
        assertEquals(1, quotation.lines().size());
        assertEquals(MeasurementSource.MANUAL, quotation.getMeasurementSource());
    }

    @Test
    void builderKeepsWhereTheMeasurementsCameFrom() {
        Quotation quotation = Quotation.builder().projectId(UUID.randomUUID())
                .location("Pasto", new BigDecimal("6.00"))
                .workingDays(26, Quotation.DaysSource.STANDARD_PLAN)
                .measurementSource(MeasurementSource.OCR)
                .takeoff(new MaterialTakeoff(needs, List.of()))
                .cost(new MaterialsCost(needs))
                .build();

        assertEquals(MeasurementSource.OCR, quotation.getMeasurementSource());
    }

    @Test
    void aQuotationWithoutMaterialsIsNotBuilt() {
        Quotation.Builder builder = Quotation.builder().projectId(UUID.randomUUID())
                .location("Pasto", new BigDecimal("6.00"))
                .workingDays(26, Quotation.DaysSource.STANDARD_PLAN)
                .cost(new MaterialsCost(List.of()));

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void theCostIsRequired() {
        Quotation.Builder builder = Quotation.builder().projectId(UUID.randomUUID())
                .location("Pasto", new BigDecimal("6.00"))
                .workingDays(26, Quotation.DaysSource.PHASES)
                .takeoff(new MaterialTakeoff(needs, List.of()));

        assertThrows(NullPointerException.class, builder::build);
    }
}
