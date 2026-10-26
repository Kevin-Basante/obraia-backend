package com.obraia.backend.entity.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.obraia.backend.entity.catalog.ElementType;

class MeasurementRulesTest {

    private static final BigDecimal ONE = BigDecimal.ONE;

    @ParameterizedTest
    @EnumSource(value = ElementType.class, names = {"FOUNDATION", "COLUMN", "BEAM", "SLAB"})
    void concretePartsAreMeasuredInCubicMeters(ElementType element) {
        assertEquals("m3", MeasurementRules.baseUnit(element));
        assertEquals(List.of("lengthM", "widthM", "heightM"), MeasurementRules.requiredDimensions(element));
    }

    @ParameterizedTest
    @EnumSource(value = ElementType.class, names = {"WALL", "FLOOR", "ROOF"})
    void surfacesAreMeasuredInSquareMeters(ElementType element) {
        assertEquals("m2", MeasurementRules.baseUnit(element));
    }

    @Test
    void wallNeedsHeightButNotWidth() {
        assertEquals(List.of("heightM"), MeasurementRules.missingDimensions(ElementType.WALL, null, null));
        assertTrue(MeasurementRules.missingDimensions(ElementType.WALL, null, ONE).isEmpty());
    }

    @Test
    void floorNeedsWidthButNotHeight() {
        assertEquals(List.of("widthM"), MeasurementRules.missingDimensions(ElementType.FLOOR, null, null));
        assertTrue(MeasurementRules.missingDimensions(ElementType.ROOF, ONE, null).isEmpty());
    }

    @Test
    void slabNeedsBoth() {
        assertEquals(List.of("widthM", "heightM"), MeasurementRules.missingDimensions(ElementType.SLAB, null, null));
    }

    @Test
    void wallAmountIsLengthTimesHeightTimesQuantity() {
        BigDecimal amount = MeasurementRules.amount(ElementType.WALL, new BigDecimal("3.50"), new BigDecimal("0.15"),
                new BigDecimal("2.40"), 4);

        assertEquals(new BigDecimal("33.60"), amount);
    }

    @Test
    void columnAmountIsAVolumeRoundedToTwoDecimals() {
        BigDecimal amount = MeasurementRules.amount(ElementType.COLUMN, new BigDecimal("0.25"),
                new BigDecimal("0.25"), new BigDecimal("2.45"), 6);

        // 0.25 x 0.25 x 2.45 x 6 = 0.91875
        assertEquals(new BigDecimal("0.92"), amount);
    }

    @Test
    void floorAmountIgnoresTheHeight() {
        BigDecimal amount = MeasurementRules.amount(ElementType.FLOOR, new BigDecimal("8.00"),
                new BigDecimal("10.00"), new BigDecimal("0.10"), 1);

        assertEquals(new BigDecimal("80.00"), amount);
    }
}
