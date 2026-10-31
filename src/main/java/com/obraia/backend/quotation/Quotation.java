package com.obraia.backend.quotation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.obraia.backend.entity.catalog.ElementType;

/**
 * Result of quoting a project. It is put together step by step with a
 * {@link Builder}, because it has many parts and some are optional.
 */
public final class Quotation {

    /** Where the working days of the labor come from. */
    public enum DaysSource {
        /** The dates of the phases the project already has. */
        PHASES,
        /** The standard phases of the project type, when the plan has no dates yet. */
        STANDARD_PLAN
    }

    private final UUID projectId;
    private final String city;
    private final BigDecimal transportPct;
    private final int workingDays;
    private final DaysSource daysSource;
    private final List<MaterialNeed> materials;
    private final List<ElementType> elementsWithoutFormulas;
    private final CostComponent cost;

    private Quotation(Builder builder) {
        this.projectId = builder.projectId;
        this.city = builder.city;
        this.transportPct = builder.transportPct;
        this.workingDays = builder.workingDays;
        this.daysSource = builder.daysSource;
        this.materials = List.copyOf(builder.materials);
        this.elementsWithoutFormulas = List.copyOf(builder.elementsWithoutFormulas);
        this.cost = builder.cost;
    }

    public static Builder builder() {
        return new Builder();
    }

    public BigDecimal total() {
        return cost.amount();
    }

    public List<CostLine> lines() {
        return cost.lines();
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getCity() {
        return city;
    }

    public BigDecimal getTransportPct() {
        return transportPct;
    }

    public int getWorkingDays() {
        return workingDays;
    }

    public DaysSource getDaysSource() {
        return daysSource;
    }

    public List<MaterialNeed> getMaterials() {
        return materials;
    }

    public List<ElementType> getElementsWithoutFormulas() {
        return elementsWithoutFormulas;
    }

    public CostComponent getCost() {
        return cost;
    }

    /** Builder of the quotation. {@link #build()} checks that the required parts are there. */
    public static final class Builder {

        private UUID projectId;
        private String city;
        private BigDecimal transportPct;
        private int workingDays;
        private DaysSource daysSource;
        private List<MaterialNeed> materials = List.of();
        private List<ElementType> elementsWithoutFormulas = List.of();
        private CostComponent cost;

        private Builder() {
        }

        public Builder projectId(UUID projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder location(String city, BigDecimal transportPct) {
            this.city = city;
            this.transportPct = transportPct;
            return this;
        }

        public Builder workingDays(int workingDays, DaysSource daysSource) {
            this.workingDays = workingDays;
            this.daysSource = daysSource;
            return this;
        }

        public Builder takeoff(MaterialTakeoff takeoff) {
            this.materials = takeoff.needs();
            this.elementsWithoutFormulas = takeoff.elementsWithoutFormulas();
            return this;
        }

        public Builder cost(CostComponent cost) {
            this.cost = cost;
            return this;
        }

        public Quotation build() {
            Objects.requireNonNull(projectId, "projectId");
            Objects.requireNonNull(city, "city");
            Objects.requireNonNull(daysSource, "daysSource");
            Objects.requireNonNull(cost, "cost");
            if (materials.isEmpty()) {
                throw new IllegalStateException("A quotation needs at least one material");
            }
            return new Quotation(this);
        }
    }
}
