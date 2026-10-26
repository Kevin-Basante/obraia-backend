package com.obraia.backend.dto.blueprint;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manual measurements against the AI ones, by element and floor.
 *
 * @param tolerancePct largest difference counted as a match
 * @param aiAvailable  false until the AI has measured some blueprint
 */
public record ComparisonResponse(BigDecimal tolerancePct, boolean aiAvailable, List<ComparisonRowResponse> rows) {
}
