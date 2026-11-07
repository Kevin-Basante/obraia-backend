package com.obraia.backend.dto.overview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.obraia.backend.dto.phase.PhaseResponse;
import com.obraia.backend.dto.phase.Progress;
import com.obraia.backend.dto.phase.TaskResponse;
import com.obraia.backend.dto.phase.WorkPlanResponse;
import com.obraia.backend.dto.project.StatusResponse;

class WorkOverviewsTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 16);

    @Test
    void countsPhasesTasksAndLateWork() {
        PhaseResponse prelim = phase(1, "Preliminares", "completed",
                task("Limpieza", "completed", DAY, false, "100.00"));
        PhaseResponse digging = phase(2, "Excavación", "in_progress",
                task("Zapatas", "in_progress", DAY.plusDays(5), true, "50.00"),
                task("Retiro", "planned", DAY.plusDays(6), false, "0.00"));

        WorkOverview work = WorkOverviews.from(WorkPlanResponse.of(List.of(prelim, digging)));

        assertEquals(new BigDecimal("50.00"), work.progressPct());
        assertEquals(2, work.phaseCount());
        assertEquals(1, work.completedPhases());
        assertEquals(3, work.taskCount());
        assertEquals(1, work.completedTasks());
        assertEquals(1, work.lateTasks());
        assertEquals("Excavación", work.currentPhase());
        assertEquals(List.of("Preliminares", "Excavación"),
                work.phases().stream().map(PhaseProgress::name).toList());
        assertEquals(2, work.phases().get(1).taskCount());
    }

    @Test
    void upcomingTasksSkipTheCompletedOnesAndPutLateOnesFirst() {
        PhaseResponse first = phase(1, "Cimentación", "in_progress",
                task("Solado", "completed", DAY, false, "100.00"),
                task("Vaciado", "planned", DAY.plusDays(3), false, "0.00"),
                task("Sin fecha", "planned", null, false, "0.00"));
        PhaseResponse second = phase(2, "Muros", "planned",
                task("Atrasada", "delayed", DAY.plusDays(9), true, "10.00"),
                task("Pronto", "planned", DAY.plusDays(1), false, "0.00"));

        List<UpcomingTask> upcoming = WorkOverviews.from(WorkPlanResponse.of(List.of(first, second)))
                .upcomingTasks();

        assertEquals(List.of("Atrasada", "Pronto", "Vaciado", "Sin fecha"),
                upcoming.stream().map(UpcomingTask::name).toList());
        assertEquals("Muros", upcoming.get(0).phaseName());
    }

    @Test
    void upcomingTasksStopAtTheLimit() {
        TaskResponse[] tasks = new TaskResponse[8];
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = task("Tarea " + i, "planned", DAY.plusDays(i), false, "0.00");
        }

        WorkOverview work = WorkOverviews.from(WorkPlanResponse.of(List.of(phase(1, "Obra", "planned", tasks))));

        assertEquals(WorkOverviews.UPCOMING_LIMIT, work.upcomingTasks().size());
        assertEquals("Tarea 0", work.upcomingTasks().get(0).name());
    }

    @Test
    void firstPlannedPhaseIsCurrentWhenNothingMoves() {
        PhaseResponse done = phase(1, "Preliminares", "completed");
        PhaseResponse next = phase(2, "Excavación", "planned");

        assertEquals("Excavación", WorkOverviews.from(WorkPlanResponse.of(List.of(done, next))).currentPhase());
    }

    @Test
    void emptyPlanHasNoCurrentPhase() {
        WorkOverview work = WorkOverviews.from(WorkPlanResponse.of(List.of()));

        assertNull(work.currentPhase());
        assertEquals(0, work.taskCount());
        assertTrue(work.upcomingTasks().isEmpty());
    }

    private static PhaseResponse phase(int order, String name, String status, TaskResponse... tasks) {
        List<TaskResponse> list = List.of(tasks);
        return new PhaseResponse(UUID.randomUUID(), name, order, new StatusResponse(status, status), List.of(), DAY,
                DAY.plusDays(10), null, null, false,
                Progress.average(list.stream().map(TaskResponse::progressPct).toList()), list);
    }

    private static TaskResponse task(String name, String status, LocalDate start, boolean late, String progress) {
        return new TaskResponse(UUID.randomUUID(), UUID.randomUUID(), name, new StatusResponse(status, status),
                List.of(), start, start == null ? null : start.plusDays(2), null, null, late, new BigDecimal(progress),
                0);
    }
}
