package com.obraia.backend.quotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.budget.BudgetItemType;
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

    @Test
    void transportIsAPercentageOfMaterialsAndWaste() {
        CostComponent cost = new TransportCost(new WasteCost(new MaterialsCost(needs), needs),
                new BigDecimal("3.00"), "Cali");

        CostLine transport = cost.lines().get(2);
        assertEquals(new BigDecimal("7119.00"), transport.amount());
        assertEquals(new BigDecimal("3.00"), transport.percentage());
        assertEquals("Transport of the materials to the site in Cali", transport.description());
    }

    @Test
    void laborIsWorkingDaysTimesTheDailyRate() {
        CostComponent cost = new LaborCost(new MaterialsCost(needs), 10, new BigDecimal("280000"));

        CostLine labor = cost.lines().get(1);
        assertEquals(new BigDecimal("2800000.00"), labor.amount());
        assertEquals("Crew for 10 working days at COP 280,000 a day", labor.description());
    }

    @Test
    void taxIsOnlyChargedOnLaborAndTransport() {
        CostComponent cost = fullStack();

        // 19 % of 2,800,000 + 7,119; materials already include VAT.
        assertEquals(new BigDecimal("533352.61"), cost.lines().get(4).amount());
    }

    @Test
    void eachLayerAddsOneLineInTheOrderItWasWrapped() {
        CostComponent cost = fullStack();

        assertEquals(List.of(BudgetItemType.MATERIALS, BudgetItemType.WASTE, BudgetItemType.TRANSPORT,
                BudgetItemType.LABOR, BudgetItemType.TAX), cost.lines().stream().map(CostLine::type).toList());
        assertEquals(new BigDecimal("3577771.61"), cost.amount());
        assertEquals(cost.amount(), cost.lines().stream().map(CostLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void taxWithoutLaborOrTransportIsZero() {
        CostComponent cost = new TaxCost(new MaterialsCost(needs), new BigDecimal("19"));

        assertEquals(new BigDecimal("0.00"), cost.lines().get(1).amount());
        assertEquals(new BigDecimal("226000.00"), cost.amount());
    }

    private CostComponent fullStack() {
        return new TaxCost(new LaborCost(new TransportCost(new WasteCost(new MaterialsCost(needs), needs),
                new BigDecimal("3.00"), "Cali"), 10, new BigDecimal("280000")), new BigDecimal("19"));
    }
}
