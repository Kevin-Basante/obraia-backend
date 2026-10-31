package com.obraia.backend.entity.phase;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.obraia.backend.exception.BadRequestException;

/**
 * Status of a phase or a task. A delayed one can go back to work or be
 * completed, and a completed one can be reopened if something was missing.
 */
public enum WorkStatus {

    PLANNED("planned", "Planned"),
    IN_PROGRESS("in_progress", "In progress"),
    DELAYED("delayed", "Delayed"),
    COMPLETED("completed", "Completed");

    private final String code;
    private final String label;

    WorkStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /** Statuses this one can change to. */
    public Set<WorkStatus> nextStatuses() {
        return switch (this) {
            case PLANNED -> EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(DELAYED, COMPLETED);
            case DELAYED -> EnumSet.of(IN_PROGRESS, COMPLETED);
            case COMPLETED -> EnumSet.of(IN_PROGRESS);
        };
    }

    public boolean canChangeTo(WorkStatus next) {
        return nextStatuses().contains(next);
    }

    public static WorkStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("INVALID_WORK_STATUS",
                        "Unknown status: " + code + ". Valid values: " + Arrays.stream(values())
                                .map(WorkStatus::getCode).collect(Collectors.joining(", "))));
    }
}
