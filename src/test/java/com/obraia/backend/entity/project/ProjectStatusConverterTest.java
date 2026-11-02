package com.obraia.backend.entity.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ProjectStatusConverterTest {

    private final ProjectStatusConverter converter = new ProjectStatusConverter();

    @Test
    void storesTheCodeAllowedByTheCheckConstraint() {
        assertEquals("in_progress", converter.convertToDatabaseColumn(ProjectStatus.IN_PROGRESS));
        assertEquals(ProjectStatus.IN_PROGRESS, converter.convertToEntityAttribute("in_progress"));
    }

    @Test
    void keepsNullAsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
