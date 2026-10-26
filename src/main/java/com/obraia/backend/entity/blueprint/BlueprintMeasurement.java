package com.obraia.backend.entity.blueprint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.obraia.backend.entity.catalog.ElementType;
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
 * One measured element of the project, for example 4 walls of 3.5 m by
 * 2.4 m on floor 1. It can point to the blueprint it was read from.
 */
@Entity
@Table(name = "blueprint_measurements")
public class BlueprintMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id")
    private Blueprint blueprint;

    @Column(name = "element_type", nullable = false, length = 15)
    private ElementType elementType;

    @Column(name = "floor_number", nullable = false)
    private short floorNumber;

    @Column(name = "length_m", nullable = false, precision = 8, scale = 2)
    private BigDecimal lengthM;

    @Column(name = "width_m", precision = 8, scale = 2)
    private BigDecimal widthM;

    @Column(name = "height_m", precision = 8, scale = 2)
    private BigDecimal heightM;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "source", nullable = false, length = 10, updatable = false)
    private MeasurementSource source;

    /** Difference with the other source, in percent. Filled when the AI measures (block 7). */
    @Column(name = "discrepancy_pct", precision = 6, scale = 2)
    private BigDecimal discrepancyPct;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected BlueprintMeasurement() {
        // Required by JPA
    }

    public BlueprintMeasurement(Project project, MeasurementSource source) {
        this.project = project;
        this.source = source;
    }

    // Not final, for the same reason as Project.update.
    public void update(Blueprint blueprint, ElementType elementType, int floorNumber, BigDecimal lengthM,
            BigDecimal widthM, BigDecimal heightM, int quantity, String notes) {
        this.blueprint = blueprint;
        this.elementType = elementType;
        this.floorNumber = (short) floorNumber;
        this.lengthM = lengthM;
        this.widthM = widthM;
        this.heightM = heightM;
        this.quantity = quantity;
        this.notes = notes;
    }

    /** Amount in the base unit of the element (m3 or m2). */
    public BigDecimal getAmount() {
        return MeasurementRules.amount(elementType, lengthM, widthM, heightM, quantity);
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public Blueprint getBlueprint() {
        return blueprint;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public BigDecimal getLengthM() {
        return lengthM;
    }

    public BigDecimal getWidthM() {
        return widthM;
    }

    public BigDecimal getHeightM() {
        return heightM;
    }

    public int getQuantity() {
        return quantity;
    }

    public MeasurementSource getSource() {
        return source;
    }

    public BigDecimal getDiscrepancyPct() {
        return discrepancyPct;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
