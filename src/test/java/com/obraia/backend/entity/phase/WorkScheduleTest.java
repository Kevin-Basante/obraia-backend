package com.obraia.backend.entity.phase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class WorkScheduleTest {

    private static final LocalDate DAY_1 = LocalDate.of(2026, 11, 2);
    private static final LocalDate DAY_5 = LocalDate.of(2026, 11, 6);

    @Test
    void newScheduleIsPlanned() {
        WorkSchedule schedule = new WorkSchedule(DAY_1, DAY_5);

        assertEquals(WorkStatus.PLANNED, schedule.getStatus());
        assertNull(schedule.getActualStartDate());
    }

    @Test
    void startingRecordsTheRealStart() {
        WorkSchedule schedule = new WorkSchedule(DAY_1, DAY_5);

        schedule.changeStatus(WorkStatus.IN_PROGRESS, DAY_1.plusDays(1));

        assertEquals(DAY_1.plusDays(1), schedule.getActualStartDate());
        assertNull(schedule.getActualEndDate());
    }

    @Test
    void completingRecordsTheEndAndKeepsTheStart() {
        WorkSchedule schedule = new WorkSchedule(DAY_1, DAY_5);
        schedule.changeStatus(WorkStatus.IN_PROGRESS, DAY_1);

        schedule.changeStatus(WorkStatus.COMPLETED, DAY_5);

        assertEquals(DAY_1, schedule.getActualStartDate());
        assertEquals(DAY_5, schedule.getActualEndDate());
    }

    @Test
    void reopeningClearsTheEnd() {
        WorkSchedule schedule = new WorkSchedule(DAY_1, DAY_5);
        schedule.changeStatus(WorkStatus.IN_PROGRESS, DAY_1);
        schedule.changeStatus(WorkStatus.COMPLETED, DAY_5);

        schedule.changeStatus(WorkStatus.IN_PROGRESS, DAY_5.plusDays(1));

        assertNull(schedule.getActualEndDate());
        assertEquals(DAY_1, schedule.getActualStartDate());
    }

    @Test
    void lateWhenTheEndPassedWithoutFinishing() {
        WorkSchedule schedule = new WorkSchedule(DAY_1, DAY_5);

        assertFalse(schedule.isLate(DAY_5));
        assertTrue(schedule.isLate(DAY_5.plusDays(1)));

        schedule.changeStatus(WorkStatus.IN_PROGRESS, DAY_1);
        schedule.changeStatus(WorkStatus.COMPLETED, DAY_5.plusDays(2));
        assertFalse(schedule.isLate(DAY_5.plusDays(3)));
    }

    @Test
    void withoutPlannedEndItIsNeverLate() {
        assertFalse(new WorkSchedule(null, null).isLate(DAY_5));
    }

    @Test
    void planChangesTheDates() {
        WorkSchedule schedule = new WorkSchedule(DAY_1, DAY_5);

        schedule.plan(DAY_5, null);

        assertEquals(DAY_5, schedule.getPlannedStartDate());
        assertNull(schedule.getPlannedEndDate());
    }
}
