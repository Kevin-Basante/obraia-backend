package com.obraia.backend.entity.phase;

import java.time.Instant;
import java.util.UUID;

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

/** A piece of work inside a phase, for example "Fundir zapatas". Its progress comes from the reports. */
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    @Column(nullable = false, length = 150)
    private String name;

    @Embedded
    private WorkSchedule schedule;

    /**
     * Set by the service, not by the database: the tasks created together
     * get one microsecond apart, so they keep the order of the plan.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected Task() {
        // Required by JPA
    }

    public Task(Phase phase, String name, WorkSchedule schedule, Instant createdAt) {
        this.phase = phase;
        this.name = name;
        this.schedule = schedule;
        this.createdAt = createdAt;
    }

    // Not final, for the same reason as Project.update.
    public void update(Phase phase, String name) {
        this.phase = phase;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public Phase getPhase() {
        return phase;
    }

    public String getName() {
        return name;
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
