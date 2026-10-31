package com.obraia.backend.quotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.catalog.MaterialCategory;
import com.obraia.backend.support.CatalogFixtures;

class CostDecoratorTest {

    // 100 blocks + 5 of waste, 2 bags of cement + 0.10 of waste.
    private final List<MaterialNeed> needs = List.of(
            new MaterialNeed(CatalogFixtures.material("Bloque de arcilla No. 5", MaterialCategory.MASONRY, "1600"),
                    new BigDecimal("100.00"), new BigDecimal("5.00")),
            new MaterialNeed(CatalogFixtures.material("Cemento gris", MaterialCategory.CEMENT_CONCRETE, "33000"),
                    new BigDecimal("2.00"), new BigDecimal("0.10")));

    @Test
    void materialsAloneOnlyCountTheExactQuantities() {
        CostComponent materials = new MaterialsCost(needs);

        assertEquals(new BigDecimal("226000.00"), materials.amount());
        assertEquals(1, materials.lines().size());
        assertEquals("Materials for the measured elements (2 items)", materials.lines().get(0).description());
    }

    @Test
    void wasteAddsTheExtraMaterial() {
        CostComponent cost = new WasteCost(new MaterialsCost(needs), needs);

        assertEquals(new BigDecimal("237300.00"), cost.amount());
        assertEquals(new BigDecimal("11300.00"), cost.lines().get(1).amount());
    }

}
