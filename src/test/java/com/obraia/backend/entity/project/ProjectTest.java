package com.obraia.backend.entity.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.catalog.SizeCategory;
import com.obraia.backend.support.ProjectFixtures;

class ProjectTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 2);

    private final Project project = ProjectFixtures.project(UUID.randomUUID(), "Casa Prueba");

    @Test
    void newProjectsStartAsDraft() {
        assertEquals(ProjectStatus.DRAFT, project.getStatus());
        assertNull(project.getActualStartDate());
    }

    @Test
    void firstStartIsRecordedAndKeptAfterAPause() {
        project.changeStatus(ProjectStatus.PLANNED, START.minusDays(5));
        project.changeStatus(ProjectStatus.IN_PROGRESS, START);
        project.changeStatus(ProjectStatus.PAUSED, START.plusDays(10));
        project.changeStatus(ProjectStatus.IN_PROGRESS, START.plusDays(20));

        assertEquals(START, project.getActualStartDate());
        assertNull(project.getActualEndDate());
    }

    @Test
    void completingRecordsTheEndDate() {
        project.changeStatus(ProjectStatus.IN_PROGRESS, START);
        project.changeStatus(ProjectStatus.COMPLETED, START.plusDays(90));

        assertEquals(START.plusDays(90), project.getActualEndDate());
    }

    @Test
    void sizeCategoryFollowsTheArea() {
        assertEquals(SizeCategory.SMALL, project.getSizeCategory());

        project.update(project.getProjectType(), project.getLocation(), "Casa Prueba", null, new BigDecimal("250"), 2,
                null, null);

        assertEquals(SizeCategory.LARGE, project.getSizeCategory());
    }
}
