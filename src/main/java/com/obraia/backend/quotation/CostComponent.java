package com.obraia.backend.quotation;

import java.math.BigDecimal;
import java.util.List;

/**
 * Component of the Decorator pattern: something that has a cost. The
 * materials are the base component; waste, transport, labor and tax are
 * decorators that wrap it and add their own layer.
 */
public interface CostComponent {

    /** Total of this component and every layer it wraps. */
    BigDecimal amount();

    /** One line per layer, from the innermost (materials) to the outermost. */
    List<CostLine> lines();
}
