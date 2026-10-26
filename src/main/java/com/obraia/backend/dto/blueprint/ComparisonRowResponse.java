package com.obraia.backend.dto.blueprint;

import java.math.BigDecimal;

/**
 * Totals of one element on one floor, typed by the user and read by the
 * AI. differencePct is null when one of the two is missing.
 *
 * @param status manual_only, ai_only, match or review
 */
public record ComparisonRowResponse(String elementType, String elementLabel, int floorNumber, String baseUnit,
        BigDecimal manualAmount, BigDecimal aiAmount, BigDecimal differencePct, String status) {
}
