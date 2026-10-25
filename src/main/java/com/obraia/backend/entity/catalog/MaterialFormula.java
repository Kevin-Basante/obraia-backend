package com.obraia.backend.entity.catalog;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * How much of a material one unit of a building element needs, for
 * example 12.5 blocks per m2 of wall. Quotations multiply it by the
 * measurements of the blueprint (block 6).
 */
@Entity
@Table(name = "material_formulas")
public class MaterialFormula {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_type_id", nullable = false)
    private ProjectType projectType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "element_type", nullable = false, length = 15)
    private ElementType elementType;

    @Column(name = "consumption_per_unit", nullable = false, precision = 12, scale = 4)
    private BigDecimal consumptionPerUnit;

    @Column(name = "base_unit", nullable = false, length = 5)
    private String baseUnit;

    /** Extra share bought to cover breakage and cuts (0.05 = 5 %). */
    @Column(name = "waste_factor", nullable = false, precision = 5, scale = 4)
    private BigDecimal wasteFactor;

    protected MaterialFormula() {
        // Required by JPA
    }

    public UUID getId() {
        return id;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public Material getMaterial() {
        return material;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public BigDecimal getConsumptionPerUnit() {
        return consumptionPerUnit;
    }

    public String getBaseUnit() {
        return baseUnit;
    }

    public BigDecimal getWasteFactor() {
        return wasteFactor;
    }
}
