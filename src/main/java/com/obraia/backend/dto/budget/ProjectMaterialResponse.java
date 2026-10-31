package com.obraia.backend.dto.budget;

import java.math.BigDecimal;
import java.util.UUID;

import com.obraia.backend.entity.budget.ProjectMaterial;
import com.obraia.backend.quotation.MaterialNeed;

/**
 * A material the project needs, waste included.
 *
 * @param unitPrice price per unit used in the quotation, VAT included
 */
public record ProjectMaterialResponse(UUID materialId, String name, String unit, BigDecimal quantity,
        BigDecimal unitPrice, BigDecimal subtotal) {

    public static ProjectMaterialResponse from(ProjectMaterial row) {
        return new ProjectMaterialResponse(row.getMaterial().getId(), row.getMaterial().getName(),
                row.getMaterial().getUnit(), row.getCalculatedQuantity(), row.getUnitPriceSnapshot(),
                row.getSubtotal());
    }

    public static ProjectMaterialResponse from(MaterialNeed need) {
        BigDecimal price = need.material().getReferencePrice();
        return new ProjectMaterialResponse(need.material().getId(), need.material().getName(),
                need.material().getUnit(), need.totalQuantity(), price, need.baseCost().add(need.wasteCost()));
    }
}
