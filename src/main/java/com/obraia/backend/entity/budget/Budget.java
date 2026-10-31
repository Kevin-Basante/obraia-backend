package com.obraia.backend.entity.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
 * One version of the cost of a project. Every quotation creates a new
 * version; the old ones are kept so the user can compare them.
 */
@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(nullable = false, updatable = false)
    private int version;

    @Column(nullable = false, length = 12)
    private BudgetStatus status;

    @Column(name = "projected_amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal projectedAmount;

    /** What was really spent. It stays at 0 until expenses are recorded (block 8). */
    @Column(name = "actual_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Budget() {
        // Required by JPA
    }

    public Budget(Project project, int version, BigDecimal projectedAmount, Instant createdAt) {
        this.project = project;
        this.version = version;
        this.status = BudgetStatus.DRAFT;
        this.projectedAmount = projectedAmount;
        this.actualAmount = BigDecimal.ZERO;
        this.createdAt = createdAt;
    }

    // Not final, for the same reason as Project.update.
    public void approve(Instant when) {
        this.status = BudgetStatus.APPROVED;
        this.approvedAt = when;
    }

    public void supersede() {
        this.status = BudgetStatus.SUPERSEDED;
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public int getVersion() {
        return version;
    }

    public BudgetStatus getStatus() {
        return status;
    }

    public BigDecimal getProjectedAmount() {
        return projectedAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
