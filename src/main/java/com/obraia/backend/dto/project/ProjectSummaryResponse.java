package com.obraia.backend.dto.project;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.obraia.backend.entity.project.Project;

/** Row of the project list. */
public record ProjectSummaryResponse(UUID id, String name, String projectTypeName, String city,
        BigDecimal builtAreaM2, String sizeCategory, StatusResponse status,
        LocalDate plannedStartDate, LocalDate plannedEndDate, Instant createdAt) {

    public static ProjectSummaryResponse from(Project project) {
        return new ProjectSummaryResponse(project.getId(), project.getName(), project.getProjectType().getName(),
                project.getLocation().getCity(), project.getBuiltAreaM2(), project.getSizeCategory().getCode(),
                StatusResponse.from(project.getStatus()), project.getPlannedStartDate(),
                project.getPlannedEndDate(), project.getCreatedAt());
    }
}
