package com.obraia.backend.entity.blueprint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.obraia.backend.entity.catalog.ElementType;

/**
 * How each building element is measured. Concrete parts (foundation,
 * columns, beams, slabs) are counted in m3; walls, floor and roof in m2,
 * the same base units as the material formulas of the catalog.
 */
public final class MeasurementRules {

    public static final String CUBIC_METERS = "m3";
    public static final String SQUARE_METERS = "m2";

    private MeasurementRules() {
    }

    public static String baseUnit(ElementType element) {
        return switch (element) {
            case FOUNDATION, COLUMN, BEAM, SLAB -> CUBIC_METERS;
            case WALL, FLOOR, ROOF -> SQUARE_METERS;
        };
    }

    /** Dimensions a measurement of this element needs, in the order of the form. */
    public static List<String> requiredDimensions(ElementType element) {
        return switch (element) {
            case FOUNDATION, COLUMN, BEAM, SLAB -> List.of("lengthM", "widthM", "heightM");
            case WALL -> List.of("lengthM", "heightM");
            case FLOOR, ROOF -> List.of("lengthM", "widthM");
        };
    }

    /**
     * Names of the dimensions this element needs and the measurement lacks.
     * The length is always required by the database.
     */
    public static List<String> missingDimensions(ElementType element, BigDecimal widthM, BigDecimal heightM) {
        List<String> required = requiredDimensions(element);
        List<String> missing = new ArrayList<>();
        if (required.contains("widthM") && widthM == null) {
            missing.add("widthM");
        }
        if (required.contains("heightM") && heightM == null) {
            missing.add("heightM");
        }
        return missing;
    }

    /**
     * Amount in the base unit: length x width x height for concrete parts,
     * length x height for walls, length x width for floor and roof; times
     * the quantity, rounded to 2 decimals.
     */
    public static BigDecimal amount(ElementType element, BigDecimal lengthM, BigDecimal widthM, BigDecimal heightM,
            int quantity) {
        BigDecimal single = switch (element) {
            case FOUNDATION, COLUMN, BEAM, SLAB -> lengthM.multiply(widthM).multiply(heightM);
            case WALL -> lengthM.multiply(heightM);
            case FLOOR, ROOF -> lengthM.multiply(widthM);
        };
        return single.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }
}
