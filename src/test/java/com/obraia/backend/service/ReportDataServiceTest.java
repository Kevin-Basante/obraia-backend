package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.config.ClockConfig;
import com.obraia.backend.dto.overview.ActivityCounts;
import com.obraia.backend.dto.overview.ProjectOverviewResponse;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.report.ReportData;
import com.obraia.backend.report.ReportType;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.support.ProjectFixtures;
import com.obraia.backend.support.ReportFixtures;

class ReportDataServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-09T15:00:00Z");

    private final Project project = ProjectFixtures.project(UUID.randomUUID(), "Casa Prueba");

    private ProjectRepository projects;
    private RiskAssessmentService risk;
    private ReportDataService service;

    @BeforeEach
    void setUp() {
        projects = mock(ProjectRepository.class);
        ProjectOverviewService overview = mock(ProjectOverviewService.class);
        risk = mock(RiskAssessmentService.class);
        when(projects.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        when(overview.overviewOf(project.getId())).thenReturn(new ProjectOverviewResponse(project.getId(),
                ReportFixtures.work(), ReportFixtures.money(), null, new ActivityCounts(1, 2, 3, 4)));
        when(risk.latestOf(project.getId())).thenReturn(Optional.of(ReportFixtures.risk()));
        service = new ReportDataService(projects, overview, risk, Clock.fixed(NOW, ClockConfig.ZONE));
    }

    @Test
    void thePublicReportGetsNoMoneyRiskOrCounts() {
        ReportData data = service.collect(project.getId(), ReportType.PUBLIC);

        assertEquals("Casa Prueba", data.project().name());
        assertEquals(1, data.floors());
        assertNotNull(data.work());
        assertNull(data.money());
        assertNull(data.risk());
        assertNull(data.counts());
        assertEquals(NOW, data.generatedAt());
        verify(risk, never()).latestOf(project.getId());
    }

    @Test
    void theFullReportGetsEverything() {
        ReportData data = service.collect(project.getId(), ReportType.FULL);

        assertNotNull(data.money());
        assertEquals("critical", data.risk().level().code());
        assertEquals(4, data.counts().members());
    }

    @Test
    void aDeletedProjectIsNotFound() {
        UUID other = UUID.randomUUID();
        when(projects.findByIdAndDeletedAtIsNull(other)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.collect(other, ReportType.PUBLIC));
    }
}
