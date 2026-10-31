package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.entity.phase.WorkStatus;
import com.obraia.backend.support.PhaseFixtures;
import com.obraia.backend.support.ProjectFixtures;

class WorkProgressTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private final Phase phase = PhaseFixtures.phase(ProjectFixtures.project(UUID.randomUUID(), "Casa"), 1,
            "Cimentación");
    private final Task first = PhaseFixtures.task(phase, "Solado de limpieza");
    private final Task second = PhaseFixtures.task(phase, "Fundida de zapatas");

    private static void complete(Task task) {
        task.getSchedule().changeStatus(WorkStatus.IN_PROGRESS, TODAY);
        task.getSchedule().changeStatus(WorkStatus.COMPLETED, TODAY);
    }

    @Test
    void phaseStaysPlannedWhileNoTaskStarted() {
        WorkProgress.syncPhase(phase, List.of(first, second), TODAY);

        assertEquals(WorkStatus.PLANNED, phase.getSchedule().getStatus());
    }

    @Test
    void phaseStartsWithItsFirstTask() {
        first.getSchedule().changeStatus(WorkStatus.IN_PROGRESS, TODAY);

        WorkProgress.syncPhase(phase, List.of(first, second), TODAY);

        assertEquals(WorkStatus.IN_PROGRESS, phase.getSchedule().getStatus());
        assertEquals(TODAY, phase.getSchedule().getActualStartDate());
    }

    @Test
    void phaseIsCompletedWithItsLastTask() {
        complete(first);
        complete(second);

        WorkProgress.syncPhase(phase, List.of(first, second), TODAY);

        assertEquals(WorkStatus.COMPLETED, phase.getSchedule().getStatus());
        assertEquals(TODAY, phase.getSchedule().getActualEndDate());
    }

    @Test
    void reopeningATaskReopensThePhase() {
        complete(first);
        complete(second);
        WorkProgress.syncPhase(phase, List.of(first, second), TODAY);

        second.getSchedule().changeStatus(WorkStatus.IN_PROGRESS, TODAY);
        WorkProgress.syncPhase(phase, List.of(first, second), TODAY);

        assertEquals(WorkStatus.IN_PROGRESS, phase.getSchedule().getStatus());
    }

    @Test
    void delayedPhaseIsNotTouchedByAStartedTask() {
        phase.getSchedule().changeStatus(WorkStatus.IN_PROGRESS, TODAY);
        phase.getSchedule().changeStatus(WorkStatus.DELAYED, TODAY);
        first.getSchedule().changeStatus(WorkStatus.IN_PROGRESS, TODAY);

        WorkProgress.syncPhase(phase, List.of(first, second), TODAY);

        assertEquals(WorkStatus.DELAYED, phase.getSchedule().getStatus());
    }

    @Test
    void phaseWithoutTasksIsLeftAsItIs() {
        WorkProgress.syncPhase(phase, List.of(), TODAY);

        assertEquals(WorkStatus.PLANNED, phase.getSchedule().getStatus());
    }

    @Test
    void allowedListsTheNextStatuses() {
        assertEquals("delayed, completed", WorkProgress.allowed(WorkStatus.IN_PROGRESS));
    }
}
