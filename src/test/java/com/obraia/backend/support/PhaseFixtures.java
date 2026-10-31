package com.obraia.backend.support;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.ProgressReport;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.entity.phase.WorkSchedule;
import com.obraia.backend.entity.project.Project;

/** Builds phases, tasks and reports for unit tests. Ids are set by reflection. */
public final class PhaseFixtures {

    private PhaseFixtures() {
    }

    public static Phase phase(Project project, int order, String name) {
        Phase phase = new Phase(project, null, name, order, new WorkSchedule(null, null));
        set(phase, "id", UUID.randomUUID());
        return phase;
    }

    public static Task task(Phase phase, String name) {
        Task task = new Task(phase, name, new WorkSchedule(null, null), Instant.parse("2026-11-02T13:00:00Z"));
        set(task, "id", UUID.randomUUID());
        return task;
    }

    public static ProgressReport report(Task task, String percentage, LocalDate date) {
        ProgressReport report = new ProgressReport(task, null, null, new BigDecimal(percentage), null, date);
        set(report, "id", UUID.randomUUID());
        return report;
    }

    private static void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
