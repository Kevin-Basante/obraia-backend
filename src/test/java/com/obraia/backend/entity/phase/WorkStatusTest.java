package com.obraia.backend.entity.phase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.obraia.backend.exception.BadRequestException;

class WorkStatusTest {

    @Test
    void plannedWorkCanOnlyStart() {
        assertTrue(WorkStatus.PLANNED.canChangeTo(WorkStatus.IN_PROGRESS));
        assertFalse(WorkStatus.PLANNED.canChangeTo(WorkStatus.COMPLETED));
    }

    @Test
    void delayedWorkCanResumeOrFinish() {
        assertTrue(WorkStatus.DELAYED.canChangeTo(WorkStatus.IN_PROGRESS));
        assertTrue(WorkStatus.DELAYED.canChangeTo(WorkStatus.COMPLETED));
    }

    @Test
    void completedWorkCanBeReopened() {
        assertTrue(WorkStatus.COMPLETED.canChangeTo(WorkStatus.IN_PROGRESS));
    }

    @ParameterizedTest
    @EnumSource(WorkStatus.class)
    void nothingGoesBackToPlanned(WorkStatus status) {
        assertFalse(status.canChangeTo(WorkStatus.PLANNED));
    }

    @Test
    void codesAreReadIgnoringCase() {
        assertEquals(WorkStatus.IN_PROGRESS, WorkStatus.fromCode(" IN_PROGRESS "));
    }

    @Test
    void unknownCodeIsABadRequest() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> WorkStatus.fromCode("done"));

        assertEquals("INVALID_WORK_STATUS", ex.getCode());
    }

    @Test
    void converterStoresTheCode() {
        WorkStatusConverter converter = new WorkStatusConverter();

        assertEquals("delayed", converter.convertToDatabaseColumn(WorkStatus.DELAYED));
        assertEquals(WorkStatus.COMPLETED, converter.convertToEntityAttribute("completed"));
    }
}
