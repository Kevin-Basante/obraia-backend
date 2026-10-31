package com.obraia.backend.entity.phase;

import java.time.Instant;
import java.util.UUID;

import com.obraia.backend.entity.project.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A stage of the work of a project, like foundation or roof, in construction order. */
@Entity
@Table(name = "phases")
public class Phase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    /** The standard phase it was copied from; null for phases that only one project type adds. */
    @Column(name = "phase_template_id", updatable = false)
    private UUID phaseTemplateId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sequence_order", nullable = false, updatable = false)
    private int sequenceOrder;

    @Embedded
    private WorkSchedule schedule;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected Phase() {
        // Required by JPA
    }

    public Phase(Project project, UUID phaseTemplateId, String name, int sequenceOrder, WorkSchedule schedule) {
        this.project = project;
        this.phaseTemplateId = phaseTemplateId;
        this.name = name;
        this.sequenceOrder = sequenceOrder;
        this.schedule = schedule;
    }

    // Not final, for the same reason as Project.update.
    public void rename(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public UUID getPhaseTemplateId() {
        return phaseTemplateId;
    }

    public String getName() {
        return name;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public WorkSchedule getSchedule() {
        return schedule;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
