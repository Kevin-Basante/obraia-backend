package com.obraia.backend.entity.budget;

import java.math.BigDecimal;
import java.util.UUID;

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
 * A row of a budget, like the labor or the transport. When the row is a
 * percentage of other rows, {@code percentage} keeps the rate used.
 */
@Entity
@Table(name = "budget_items")
public class BudgetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false, updatable = false)
    private Budget budget;

    @Column(name = "item_type", nullable = false, length = 15, updatable = false)
    private BudgetItemType itemType;

    @Column(nullable = false, updatable = false)
    private String description;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(precision = 5, scale = 2, updatable = false)
    private BigDecimal percentage;

    @Column(name = "is_percentage", nullable = false, updatable = false)
    private boolean percentageBased;

    @Column(name = "sequence_order", nullable = false, updatable = false)
    private int sequenceOrder;

    protected BudgetItem() {
        // Required by JPA
    }

    /** {@code percentage} is null for a fixed amount. */
    public BudgetItem(Budget budget, BudgetItemType itemType, String description, BigDecimal amount,
            BigDecimal percentage, int sequenceOrder) {
        this.budget = budget;
        this.itemType = itemType;
        this.description = description;
        this.amount = amount;
        this.percentage = percentage;
        this.percentageBased = percentage != null;
        this.sequenceOrder = sequenceOrder;
    }

    public UUID getId() {
        return id;
    }

    public Budget getBudget() {
        return budget;
    }

    public BudgetItemType getItemType() {
        return itemType;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public boolean isPercentageBased() {
        return percentageBased;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }
}
