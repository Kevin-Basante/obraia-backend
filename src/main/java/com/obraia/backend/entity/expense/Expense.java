package com.obraia.backend.entity.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.obraia.backend.entity.User;
import com.obraia.backend.entity.phase.Phase;
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
 * Money really spent on a project. The phase is optional; the sum of the
 * expenses is compared with the approved budget.
 */
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id", updatable = false)
    private Phase phase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by", updatable = false)
    private User registeredBy;

    @Column(nullable = false, length = 15, updatable = false)
    private ExpenseCategory category;

    @Column(nullable = false, updatable = false)
    private String description;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false, updatable = false)
    private LocalDate expenseDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Expense() {
        // Required by JPA
    }

    public Expense(Project project, Phase phase, User registeredBy, ExpenseCategory category, String description,
            BigDecimal amount, LocalDate expenseDate, Instant createdAt) {
        this.project = project;
        this.phase = phase;
        this.registeredBy = registeredBy;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public Phase getPhase() {
        return phase;
    }

    public User getRegisteredBy() {
        return registeredBy;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
