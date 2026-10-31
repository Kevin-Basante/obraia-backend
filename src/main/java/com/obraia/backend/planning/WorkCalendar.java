package com.obraia.backend.planning;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Working days of a construction site in Colombia: Monday to Saturday.
 * Sundays are skipped when a duration is turned into dates.
 */
public final class WorkCalendar {

    private WorkCalendar() {
    }

    public static boolean isWorkingDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    /** The same day if it is a working day; otherwise the next one. */
    public static LocalDate firstWorkingDayFrom(LocalDate date) {
        LocalDate day = date;
        while (!isWorkingDay(day)) {
            day = day.plusDays(1);
        }
        return day;
    }

    /** Last day of a job that starts on {@code start} and takes {@code workingDays} days. */
    public static LocalDate endDate(LocalDate start, int workingDays) {
        LocalDate day = firstWorkingDayFrom(start);
        for (int counted = 1; counted < workingDays; counted++) {
            day = firstWorkingDayFrom(day.plusDays(1));
        }
        return day;
    }

    /** First working day after {@code date}. */
    public static LocalDate nextWorkingDay(LocalDate date) {
        return firstWorkingDayFrom(date.plusDays(1));
    }

    /** Working days from {@code start} to {@code end}, both included. */
    public static int workingDaysBetween(LocalDate start, LocalDate end) {
        int days = 0;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            if (isWorkingDay(day)) {
                days++;
            }
        }
        return days;
    }
}
