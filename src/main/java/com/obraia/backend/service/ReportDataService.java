package com.obraia.backend.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.overview.ProjectOverviewResponse;
import com.obraia.backend.dto.project.ProjectSummaryResponse;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.report.ReportData;
import com.obraia.backend.report.ReportType;
import com.obraia.backend.repository.ProjectRepository;

/**
 * Gathers the figures of a report in one read-only transaction. It does not
 * check who asks: {@link ReportService} does that before calling it.
 */
@Service
public class ReportDataService {

    private final ProjectRepository projectRepository;
    private final ProjectOverviewService overviewService;
    private final RiskAssessmentService riskService;
    private final Clock clock;

    public ReportDataService(ProjectRepository projectRepository, ProjectOverviewService overviewService,
            RiskAssessmentService riskService, Clock clock) {
        this.projectRepository = projectRepository;
        this.overviewService = overviewService;
        this.riskService = riskService;
        this.clock = clock;
    }

    /** The public report gets no money, risk or counts at all, not even hidden. */
    @Transactional(readOnly = true)
    public ReportData collect(UUID projectId, ReportType type) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> ProjectAccessService.notFound(projectId));
        ProjectOverviewResponse overview = overviewService.overviewOf(projectId);
        if (type == ReportType.PUBLIC) {
            return new ReportData(type, ProjectSummaryResponse.from(project), project.getFloors(), overview.work(),
                    null, null, null, clock.instant(), null);
        }
        return new ReportData(type, ProjectSummaryResponse.from(project), project.getFloors(), overview.work(),
                overview.money(), riskService.latestOf(projectId).orElse(null), overview.counts(), clock.instant(),
                null);
    }
}
