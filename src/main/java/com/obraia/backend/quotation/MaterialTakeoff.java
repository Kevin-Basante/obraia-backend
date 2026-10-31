package com.obraia.backend.quotation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.catalog.Material;
import com.obraia.backend.entity.catalog.MaterialFormula;

/**
 * Turns the measurements into quantities of material: the amount of each
 * element (m3 or m2) times the consumption of each formula of that element.
 *
 * @param needs                   one entry per material, by name
 * @param elementsWithoutFormulas measured elements the project type has no formulas for
 */
public record MaterialTakeoff(List<MaterialNeed> needs, List<ElementType> elementsWithoutFormulas) {

    public static MaterialTakeoff of(List<BlueprintMeasurement> measurements, List<MaterialFormula> formulas) {
        Map<ElementType, BigDecimal> amounts = new EnumMap<>(ElementType.class);
        for (BlueprintMeasurement measurement : measurements) {
            amounts.merge(measurement.getElementType(), measurement.getAmount(), BigDecimal::add);
        }
        Map<UUID, Material> materials = new LinkedHashMap<>();
        Map<UUID, BigDecimal> quantities = new LinkedHashMap<>();
        Map<UUID, BigDecimal> waste = new LinkedHashMap<>();
        Set<ElementType> covered = EnumSet.noneOf(ElementType.class);
        for (MaterialFormula formula : formulas) {
            BigDecimal amount = amounts.get(formula.getElementType());
            if (amount == null) {
                continue;
            }
            covered.add(formula.getElementType());
            UUID materialId = formula.getMaterial().getId();
            BigDecimal quantity = amount.multiply(formula.getConsumptionPerUnit());
            materials.putIfAbsent(materialId, formula.getMaterial());
            quantities.merge(materialId, quantity, BigDecimal::add);
            waste.merge(materialId, quantity.multiply(formula.getWasteFactor()), BigDecimal::add);
        }
        List<MaterialNeed> needs = new ArrayList<>();
        materials.forEach((id, material) -> needs.add(new MaterialNeed(material,
                Money.round(quantities.get(id)), Money.round(waste.get(id)))));
        needs.sort(Comparator.comparing(need -> need.material().getName()));
        List<ElementType> missing = amounts.keySet().stream().filter(element -> !covered.contains(element))
                .toList();
        return new MaterialTakeoff(List.copyOf(needs), missing);
    }
}
