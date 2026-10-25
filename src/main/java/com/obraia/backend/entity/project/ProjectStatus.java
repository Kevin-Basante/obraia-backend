package com.obraia.backend.entity.project;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.obraia.backend.exception.BadRequestException;

/**
 * Life cycle of a project. Each status lists the ones it can move to;
 * completed and cancelled are final.
 */
public enum ProjectStatus {

    DRAFT("draft", "Draft"),
    PLANNED("planned", "Planned"),
    IN_PROGRESS("in_progress", "In progress"),
    PAUSED("paused", "Paused"),
    COMPLETED("completed", "Completed"),
    CANCELLED("cancelled", "Cancelled");

    private final String code;
    private final String label;

    ProjectStatus(String code, String label) {
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
    public Set<ProjectStatus> nextStatuses() {
        return switch (this) {
            case DRAFT -> EnumSet.of(PLANNED, CANCELLED);
            case PLANNED -> EnumSet.of(DRAFT, IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> EnumSet.of(PAUSED, COMPLETED, CANCELLED);
            case PAUSED -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(ProjectStatus.class);
        };
    }

    public boolean canChangeTo(ProjectStatus next) {
        return nextStatuses().contains(next);
    }

    public static ProjectStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("INVALID_PROJECT_STATUS",
                        "Unknown project status: " + code + ". Valid values: " + Arrays.stream(values())
                                .map(ProjectStatus::getCode).collect(Collectors.joining(", "))));
    }
}
