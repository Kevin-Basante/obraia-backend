package com.obraia.backend.entity.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.obraia.backend.entity.catalog.Material;
import com.obraia.backend.entity.project.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * How much of a material the project needs, waste included, with the
 * price it had in the last quotation. The database computes the subtotal.
 */
@Entity
@Table(name = "project_materials")
public class ProjectMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false, updatable = false)
    private Material material;

    @Column(name = "calculated_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal calculatedQuantity;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "subtotal", insertable = false, updatable = false, precision = 16, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected ProjectMaterial() {
        // Required by JPA
    }

    public ProjectMaterial(Project project, Material material, BigDecimal calculatedQuantity,
            BigDecimal unitPriceSnapshot) {
        this.project = project;
        this.material = material;
        this.calculatedQuantity = calculatedQuantity;
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public Material getMaterial() {
        return material;
    }

    public BigDecimal getCalculatedQuantity() {
        return calculatedQuantity;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    /** Filled by the database; quantity times price. */
    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
