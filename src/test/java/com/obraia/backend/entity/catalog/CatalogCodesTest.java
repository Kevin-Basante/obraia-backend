package com.obraia.backend.entity.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.obraia.backend.exception.BadRequestException;

/** The codes must match the values allowed by the CHECK constraints of V2. */
class CatalogCodesTest {

    @Test
    void materialCategoryIsFoundByCodeIgnoringCaseAndSpaces() {
        assertEquals(MaterialCategory.CEMENT_CONCRETE, MaterialCategory.fromCode(" Cement_Concrete "));
    }

    @Test
    void unknownMaterialCategoryListsTheValidCodes() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> MaterialCategory.fromCode("wood"));

        assertEquals("INVALID_MATERIAL_CATEGORY", ex.getCode());
        assertTrue(ex.getMessage().contains("cement_concrete, aggregates"));
    }

    @Test
    void elementTypesFollowTheConstructionOrder() {
        assertTrue(ElementType.FOUNDATION.compareTo(ElementType.WALL) < 0);
        assertTrue(ElementType.WALL.compareTo(ElementType.ROOF) < 0);
    }

    @Test
    void unknownElementTypeIsRejected() {
        assertEquals("INVALID_ELEMENT_TYPE",
                assertThrows(BadRequestException.class, () -> ElementType.fromCode("door")).getCode());
    }

    @Test
    void convertersWriteAndReadTheDatabaseCodes() {
        MaterialCategoryConverter categories = new MaterialCategoryConverter();
        SizeCategoryConverter sizes = new SizeCategoryConverter();
        ElementTypeConverter elements = new ElementTypeConverter();

        assertEquals("cement_concrete", categories.convertToDatabaseColumn(MaterialCategory.CEMENT_CONCRETE));
        assertEquals(MaterialCategory.STEEL, categories.convertToEntityAttribute("steel"));
        assertEquals("large", sizes.convertToDatabaseColumn(SizeCategory.LARGE));
        assertEquals(ElementType.SLAB, elements.convertToEntityAttribute("slab"));
        assertNull(elements.convertToDatabaseColumn(null));
    }
}
