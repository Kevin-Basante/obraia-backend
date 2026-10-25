package com.obraia.backend.dto.project;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.obraia.backend.dto.catalog.LocationResponse;
import com.obraia.backend.dto.catalog.ProjectTypeResponse;
import com.obraia.backend.entity.project.Project;

/**
 * Full data of one project. `nextStatuses` lists the statuses it can move
 * to, so the frontend only offers valid changes.
 */
public record ProjectResponse(UUID id, String name, ProjectTypeResponse projectType, LocationResponse location,
        String address, BigDecimal builtAreaM2, int floors, String sizeCategory,
        StatusResponse status, List<StatusResponse> nextStatuses,
        LocalDate plannedStartDate, LocalDate plannedEndDate, LocalDate actualStartDate, LocalDate actualEndDate,
        SiteManagerResponse siteManager, String myRole, Instant createdAt, Instant updatedAt) {

    public static ProjectResponse from(Project project, String myRole) {
        List<StatusResponse> next = project.getStatus().nextStatuses().stream().map(StatusResponse::from).toList();
        SiteManagerResponse siteManager = project.getSiteManager() == null
                ? null
                : SiteManagerResponse.from(project.getSiteManager());
        return new ProjectResponse(project.getId(), project.getName(),
                ProjectTypeResponse.from(project.getProjectType()), LocationResponse.from(project.getLocation()),
                project.getAddress(), project.getBuiltAreaM2(), project.getFloors(),
                project.getSizeCategory().getCode(), StatusResponse.from(project.getStatus()), next,
                project.getPlannedStartDate(), project.getPlannedEndDate(), project.getActualStartDate(),
                project.getActualEndDate(), siteManager, myRole, project.getCreatedAt(), project.getUpdatedAt());
    }
}
