package com.obraia.backend.entity.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.obraia.backend.exception.BadRequestException;

class SizeCategoryTest {

    @Test
    void upTo120SquareMetersIsSmall() {
        assertEquals(SizeCategory.SMALL, SizeCategory.forArea(new BigDecimal("45.5")));
        assertEquals(SizeCategory.SMALL, SizeCategory.forArea(new BigDecimal("120.00")));
    }

    @Test
    void moreThan120SquareMetersIsLarge() {
        assertEquals(SizeCategory.LARGE, SizeCategory.forArea(new BigDecimal("120.01")));
    }

    @Test
    void codeIsReadIgnoringCase() {
        assertEquals(SizeCategory.LARGE, SizeCategory.fromCode("LARGE"));
    }

    @Test
    void unknownCodeIsABadRequest() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> SizeCategory.fromCode("medium"));

        assertEquals("INVALID_SIZE_CATEGORY", ex.getCode());
    }
}
