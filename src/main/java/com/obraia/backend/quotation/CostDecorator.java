package com.obraia.backend.quotation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.obraia.backend.entity.budget.BudgetItemType;

/**
 * Base decorator: wraps another component and adds one layer of cost on
 * top of it. Subclasses only say what their own line is.
 */
public abstract class CostDecorator implements CostComponent {

    private final CostComponent inner;

    protected CostDecorator(CostComponent inner) {
        this.inner = inner;
    }

    /** The layer this decorator adds. */
    protected abstract CostLine ownLine();

    @Override
    public BigDecimal amount() {
        return inner.amount().add(ownLine().amount());
    }

    @Override
    public List<CostLine> lines() {
        List<CostLine> lines = new ArrayList<>(inner.lines());
        lines.add(ownLine());
        return lines;
    }

    /** Sum of the wrapped lines of the given types, the base of a percentage. */
    protected BigDecimal innerAmountOf(Set<BudgetItemType> types) {
        return inner.lines().stream().filter(line -> types.contains(line.type())).map(CostLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
