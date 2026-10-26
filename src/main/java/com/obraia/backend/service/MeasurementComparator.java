package com.obraia.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.obraia.backend.dto.blueprint.ComparisonResponse;
import com.obraia.backend.dto.blueprint.ComparisonRowResponse;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementRules;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.catalog.ElementType;

/**
 * Adds up the measurements of each element and floor, once for the
 * manual ones and once for the AI ones, and says how far apart they are.
 */
public final class MeasurementComparator {

    /** A difference up to 10 % counts as the same measurement. */
    public static final BigDecimal TOLERANCE_PCT = new BigDecimal("10.00");

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private MeasurementComparator() {
    }

    private record Key(ElementType element, int floor) implements Comparable<Key> {

        @Override
        public int compareTo(Key other) {
            int byElement = element.compareTo(other.element);
            return byElement != 0 ? byElement : Integer.compare(floor, other.floor);
        }
    }

    private static final class Totals {
        private BigDecimal manual;
        private BigDecimal ai;
    }

    public static ComparisonResponse compare(List<BlueprintMeasurement> measurements) {
        // Sorted by construction order, then floor.
        Map<Key, Totals> totals = new TreeMap<>();
        boolean aiAvailable = false;
        for (BlueprintMeasurement measurement : measurements) {
            Totals row = totals.computeIfAbsent(new Key(measurement.getElementType(), measurement.getFloorNumber()),
                    key -> new Totals());
            if (measurement.getSource() == MeasurementSource.OCR) {
                aiAvailable = true;
                row.ai = add(row.ai, measurement.getAmount());
            } else {
                row.manual = add(row.manual, measurement.getAmount());
            }
        }
        List<ComparisonRowResponse> rows = new ArrayList<>();
        totals.forEach((key, row) -> rows.add(toRow(key, row)));
        return new ComparisonResponse(TOLERANCE_PCT, aiAvailable, rows);
    }

    /** How much the AI amount differs from the manual one, in percent of the manual one. */
    public static BigDecimal differencePct(BigDecimal manual, BigDecimal ai) {
        return ai.subtract(manual).multiply(HUNDRED).divide(manual, 2, RoundingMode.HALF_UP);
    }

    private static ComparisonRowResponse toRow(Key key, Totals row) {
        BigDecimal difference = null;
        String status;
        if (row.ai == null) {
            status = "manual_only";
        } else if (row.manual == null) {
            status = "ai_only";
        } else {
            difference = differencePct(row.manual, row.ai);
            status = difference.abs().compareTo(TOLERANCE_PCT) <= 0 ? "match" : "review";
        }
        return new ComparisonRowResponse(key.element().getCode(), key.element().getLabel(), key.floor(),
                MeasurementRules.baseUnit(key.element()), row.manual, row.ai, difference, status);
    }

    private static BigDecimal add(BigDecimal total, BigDecimal amount) {
        return total == null ? amount : total.add(amount);
    }
}
