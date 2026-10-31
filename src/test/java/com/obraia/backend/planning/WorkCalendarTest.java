package com.obraia.backend.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class WorkCalendarTest {

    // 2026-11-01 is a Sunday.
    private static final LocalDate SUNDAY = LocalDate.of(2026, 11, 1);
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 11, 7);

    @Test
    void sundayIsTheOnlyDayOff() {
        assertFalse(WorkCalendar.isWorkingDay(SUNDAY));
        assertTrue(WorkCalendar.isWorkingDay(MONDAY));
        assertTrue(WorkCalendar.isWorkingDay(SATURDAY));
    }

    @Test
    void workStartingOnSundayMovesToMonday() {
        assertEquals(MONDAY, WorkCalendar.firstWorkingDayFrom(SUNDAY));
        assertEquals(MONDAY, WorkCalendar.firstWorkingDayFrom(MONDAY));
    }

    @Test
    void oneDayJobEndsTheSameDay() {
        assertEquals(MONDAY, WorkCalendar.endDate(MONDAY, 1));
    }

    @Test
    void sixWorkingDaysFillAWeek() {
        assertEquals(SATURDAY, WorkCalendar.endDate(MONDAY, 6));
    }

    @Test
    void endDateSkipsSundays() {
        // Saturday plus 2 working days: Saturday, (Sunday off), Monday.
        assertEquals(LocalDate.of(2026, 11, 9), WorkCalendar.endDate(SATURDAY, 2));
        assertEquals(LocalDate.of(2026, 11, 14), WorkCalendar.endDate(MONDAY, 12));
    }

    @Test
    void nextWorkingDayAfterSaturdayIsMonday() {
        assertEquals(LocalDate.of(2026, 11, 9), WorkCalendar.nextWorkingDay(SATURDAY));
        assertEquals(LocalDate.of(2026, 11, 3), WorkCalendar.nextWorkingDay(MONDAY));
    }
}
