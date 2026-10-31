package com.obraia.backend.entity.phase;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Planned and real dates of a phase or a task, with the status that the
 * dates follow. Phases and tasks share it, so both behave the same way.
 */
@Embeddable
public class WorkSchedule {

    @Column(name = "status", nullable = false, length = 15)
    private WorkStatus status = WorkStatus.PLANNED;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    protected WorkSchedule() {
        // Required by JPA
    }

    public WorkSchedule(LocalDate plannedStartDate, LocalDate plannedEndDate) {
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
    }

    // Not final: Hibernate has to see the changes (see Project.update).
    public void plan(LocalDate plannedStartDate, LocalDate plannedEndDate) {
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
    }

    /**
     * Moves to a new status and records the real dates: the first start
     * is kept, completing sets the end, and reopening clears it.
     */
    public void changeStatus(WorkStatus next, LocalDate today) {
        if (next != WorkStatus.PLANNED && actualStartDate == null) {
            actualStartDate = today;
        }
        if (next == WorkStatus.COMPLETED) {
            actualEndDate = today.isBefore(actualStartDate) ? actualStartDate : today;
        } else {
            actualEndDate = null;
        }
        this.status = next;
    }

    /** Not completed and its planned end already passed. */
    public boolean isLate(LocalDate today) {
        return status != WorkStatus.COMPLETED && plannedEndDate != null && plannedEndDate.isBefore(today);
    }

    public WorkStatus getStatus() {
        return status;
    }

    public LocalDate getPlannedStartDate() {
        return plannedStartDate;
    }

    public LocalDate getPlannedEndDate() {
        return plannedEndDate;
    }

    public LocalDate getActualStartDate() {
        return actualStartDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }
}
