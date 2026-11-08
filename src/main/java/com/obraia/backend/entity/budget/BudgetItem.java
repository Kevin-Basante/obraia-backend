package com.obraia.backend.entity.budget;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;

import com.obraia.backend.util.TextParams;
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

    /** Values of the description as JSON text; the column is jsonb, so the text is cast when it is written. */
    @Column(updatable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String params;

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
        this(budget, itemType, description, Map.of(), amount, percentage, sequenceOrder);
    }

    /** Same as above, with the values of the description so the frontend can translate it. */
    public BudgetItem(Budget budget, BudgetItemType itemType, String description, Map<String, String> params,
            BigDecimal amount, BigDecimal percentage, int sequenceOrder) {
        this.budget = budget;
        this.params = TextParams.toJson(params);
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

    /** The values of the description; null when it has none. */
    public Map<String, String> getParams() {
        return TextParams.fromJson(params);
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
