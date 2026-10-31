package com.obraia.backend.entity.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NotificationTypeTest {

    private final NotificationTypeConverter converter = new NotificationTypeConverter();

    @Test
    void everyTypeGoesAndComesBackByItsCode() {
        for (NotificationType type : NotificationType.values()) {
            assertEquals(type, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(type)));
        }
        assertEquals("budget_deviation", NotificationType.BUDGET_DEVIATION.getCode());
        assertEquals("Risk of delay", NotificationType.DELAY_RISK.getLabel());
    }

    @Test
    void nullStaysNull() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void unknownCodeFails() {
        assertThrows(IllegalArgumentException.class, () -> NotificationType.fromCode("party"));
    }
}
