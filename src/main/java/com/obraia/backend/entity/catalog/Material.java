package com.obraia.backend.entity.catalog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Construction material with its reference price in COP, VAT included. */
@Entity
@Table(name = "material_catalog")
public class Material {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(nullable = false, length = 20)
    private MaterialCategory category;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "reference_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal referencePrice;

    @Column(name = "price_updated_at", nullable = false)
    private LocalDate priceUpdatedAt;

    protected Material() {
        // Required by JPA
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MaterialCategory getCategory() {
        return category;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getReferencePrice() {
        return referencePrice;
    }

    public LocalDate getPriceUpdatedAt() {
        return priceUpdatedAt;
    }
}
