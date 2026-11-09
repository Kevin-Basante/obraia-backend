package com.obraia.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ForbiddenException;
import com.obraia.backend.report.ProjectReportWriter;
import com.obraia.backend.report.ReportData;
import com.obraia.backend.report.ReportFile;
import com.obraia.backend.report.ReportLanguage;
import com.obraia.backend.report.ReportTexts;
import com.obraia.backend.report.ReportType;

/**
 * Makes the PDF reports of a project. It checks who asks, gathers the
 * figures, adds the summary of the AI when there is one and hands
 * everything to the writer of the report.
 * The methods are not transactional on purpose: the call to the AI can take
 * a while and must not hold a connection to the database.
 */
@Service
public class ReportService {

    private final ProjectAccessService accessService;
    private final ReportRequestService requestService;
    private final ReportShareService shareService;
    private final ReportDataService dataService;
    private final ReportSummaryService summaryService;
    private final List<ProjectReportWriter> writers;

    public ReportService(ProjectAccessService accessService, ReportRequestService requestService,
            ReportShareService shareService, ReportDataService dataService, ReportSummaryService summaryService,
            List<ProjectReportWriter> writers) {
        this.accessService = accessService;
        this.requestService = requestService;
        this.shareService = shareService;
        this.dataService = dataService;
        this.summaryService = summaryService;
        this.writers = writers;
    }

    /** Any member downloads the public report. */
    public ReportFile publicReport(UUID projectId, UUID userId, String lang) {
        accessService.requireMembership(projectId, userId);
        return build(projectId, ReportType.PUBLIC, ReportLanguage.fromCode(lang));
    }

    /** The creator, or a member with an approved request that did not run out. */
    public ReportFile fullReport(UUID projectId, UUID userId, String lang) {
        Project project = accessService.requireProject(projectId, userId);
        if (!requestService.canDownloadFull(project, userId)) {
            throw new ForbiddenException("REPORT_NOT_APPROVED",
                    "The creator of the project has to approve your download of the full report");
        }
        return build(projectId, ReportType.FULL, ReportLanguage.fromCode(lang));
    }

    /** Public route: a shared link only opens the public report. */
    public ReportFile sharedReport(String token, String lang) {
        return build(shareService.projectOf(token), ReportType.PUBLIC, ReportLanguage.fromCode(lang));
    }

    private ReportFile build(UUID projectId, ReportType type, ReportLanguage language) {
        ReportData data = dataService.collect(projectId, type);
        ReportData withSummary = data.withSummary(summaryService.summarize(data, language));
        return writer(type).write(withSummary, ReportTexts.of(language));
    }

    private ProjectReportWriter writer(ReportType type) {
        return writers.stream()
                .filter(writer -> writer.type() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("There is no writer for the report " + type));
    }
}
