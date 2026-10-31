package com.obraia.backend.quotation;

import java.math.BigDecimal;

import com.obraia.backend.entity.catalog.Material;

/**
 * How much of one material the measured elements use.
 *
 * @param quantity      exact quantity, in the unit of the material
 * @param wasteQuantity extra quantity bought for breakage and cuts
 */
public record MaterialNeed(Material material, BigDecimal quantity, BigDecimal wasteQuantity) {

    public BigDecimal totalQuantity() {
        return quantity.add(wasteQuantity);
    }

    public BigDecimal baseCost() {
        return Money.round(quantity.multiply(material.getReferencePrice()));
    }

    public BigDecimal wasteCost() {
        return Money.round(wasteQuantity.multiply(material.getReferencePrice()));
    }
}
