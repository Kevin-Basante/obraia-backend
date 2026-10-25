package com.obraia.backend.entity.catalog;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Standard phase of a project type and size, in construction order.
 * New projects copy these phases (block 5).
 */
@Entity
@Table(name = "phase_templates")
public class PhaseTemplate {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_type_id", nullable = false)
    private ProjectType projectType;

    @Column(name = "size_category", nullable = false, length = 10)
    private SizeCategory sizeCategory;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Column(name = "estimated_duration_days", nullable = false)
    private int estimatedDurationDays;

    protected PhaseTemplate() {
        // Required by JPA
    }

    public UUID getId() {
        return id;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public SizeCategory getSizeCategory() {
        return sizeCategory;
    }

    public String getName() {
        return name;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public int getEstimatedDurationDays() {
        return estimatedDurationDays;
    }
}
