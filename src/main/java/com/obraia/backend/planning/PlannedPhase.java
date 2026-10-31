package com.obraia.backend.planning;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A phase proposed by a planner, before it is saved.
 *
 * @param templateId   standard phase it comes from, or null for a phase of one project type only
 * @param plannedStart null when the project has no planned start date
 * @param tasks        names of the tasks it starts with
 */
public record PlannedPhase(UUID templateId, String name, int order, int durationDays, LocalDate plannedStart,
        LocalDate plannedEnd, List<String> tasks) {
}
