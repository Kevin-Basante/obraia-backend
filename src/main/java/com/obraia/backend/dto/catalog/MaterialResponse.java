package com.obraia.backend.dto.catalog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.obraia.backend.entity.catalog.Material;

/** Material of the catalog. The price is in COP and includes VAT. */
public record MaterialResponse(UUID id, String name, CategoryResponse category, String unit,
        BigDecimal referencePrice, LocalDate priceUpdatedAt) {

    public static MaterialResponse from(Material material) {
        return new MaterialResponse(material.getId(), material.getName(),
                CategoryResponse.from(material.getCategory()), material.getUnit(),
                material.getReferencePrice(), material.getPriceUpdatedAt());
    }
}
