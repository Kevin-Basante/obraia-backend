package com.obraia.backend.entity.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.obraia.backend.exception.BadRequestException;

class ProjectStatusTest {

    @Test
    void draftCanOnlyBePlannedOrCancelled() {
        assertTrue(ProjectStatus.DRAFT.canChangeTo(ProjectStatus.PLANNED));
        assertTrue(ProjectStatus.DRAFT.canChangeTo(ProjectStatus.CANCELLED));
        assertFalse(ProjectStatus.DRAFT.canChangeTo(ProjectStatus.IN_PROGRESS));
        assertFalse(ProjectStatus.DRAFT.canChangeTo(ProjectStatus.COMPLETED));
    }

    @Test
    void workInProgressCanBePausedAndResumed() {
        assertTrue(ProjectStatus.IN_PROGRESS.canChangeTo(ProjectStatus.PAUSED));
        assertTrue(ProjectStatus.PAUSED.canChangeTo(ProjectStatus.IN_PROGRESS));
        assertFalse(ProjectStatus.PAUSED.canChangeTo(ProjectStatus.COMPLETED));
    }

    @ParameterizedTest
    @EnumSource(value = ProjectStatus.class, names = {"COMPLETED", "CANCELLED"})
    void finishedStatusesAreFinal(ProjectStatus status) {
        assertTrue(status.nextStatuses().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(ProjectStatus.class)
    void noStatusChangesToItself(ProjectStatus status) {
        assertFalse(status.canChangeTo(status));
    }

    @Test
    void codesAreReadWithoutCaseOrSpaces() {
        assertEquals(ProjectStatus.IN_PROGRESS, ProjectStatus.fromCode(" IN_PROGRESS "));
    }

    @Test
    void unknownCodeIsABadRequest() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> ProjectStatus.fromCode("done"));

        assertEquals("INVALID_PROJECT_STATUS", ex.getCode());
        assertTrue(ex.getMessage().contains("in_progress"));
    }
}
