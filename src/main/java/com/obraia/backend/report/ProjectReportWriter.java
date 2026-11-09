package com.obraia.backend.report;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.obraia.backend.dto.overview.PhaseProgress;
import com.obraia.backend.dto.overview.UpcomingTask;
import com.obraia.backend.dto.overview.WorkOverview;
import com.obraia.backend.dto.project.ProjectSummaryResponse;
import com.obraia.backend.dto.project.StatusResponse;

/**
 * Template Method of the PDF reports. {@link #write} fixes the order of
 * every report: header, facts, AI summary, progress, phases and next tasks.
 * Each subclass says which report it is and adds its own sections in
 * {@link #writeRestrictedSections}: nothing for the public report, money
 * and risk for the full one.
 */
public abstract class ProjectReportWriter {

    /** The report this writer makes. */
    public abstract ReportType type();

    /** Sections only this report has, after the progress of the work. */
    protected abstract void writeRestrictedSections(ReportDocument document, ReportData data, ReportTexts texts);

    /** The steps of every report, always in the same order. */
    public final ReportFile write(ReportData data, ReportTexts texts) {
        ProjectSummaryResponse project = data.project();
        String key = "report." + type().getCode();
        ReportDocument document = ReportDocument.open(
                texts.text("report.brand") + " · " + project.name() + " · " + texts.text(key + ".kicker"),
                texts.text("report.page"), texts.text(key + ".title") + " · " + project.name(),
                texts.text("report.brand"));

        document.header(texts.text("report.brand"), texts.text(key + ".kicker"), project.name(),
                texts.text(key + ".title") + " · "
                        + texts.text("report.generated", Map.of("date", texts.dateTime(data.generatedAt()))));
        writeFacts(document, data, texts);
        document.note(texts.text(key + ".note"));
        if (data.summary() != null) {
            writeSummary(document, data.summary(), texts);
        }
        writeProgress(document, data.work(), texts);
        writePhases(document, data.work(), texts);
        writeTasks(document, data.work(), texts);
        writeRestrictedSections(document, data, texts);
        return new ReportFile(filename(project.name()), document.finish());
    }

    private void writeFacts(ReportDocument document, ReportData data, ReportTexts texts) {
        ProjectSummaryResponse project = data.project();
        List<String[]> facts = new ArrayList<>();
        facts.add(new String[] { texts.text("facts.type"), project.projectTypeName() });
        facts.add(new String[] { texts.text("facts.city"), project.city() });
        facts.add(new String[] { texts.text("facts.status"), status(texts, project.status()) });
        facts.add(new String[] { texts.text("facts.area"),
                texts.text("facts.areaValue", Map.of("area", texts.number(project.builtAreaM2(), 2))) });
        facts.add(new String[] { texts.text("facts.floors"), String.valueOf(data.floors()) });
        // Without dates of its own, the project shows the dates of its plan.
        boolean ownDates = project.plannedStartDate() != null && project.plannedEndDate() != null;
        facts.add(new String[] { texts.text("facts.plannedDates"), ownDates
                ? texts.dateRange(project.plannedStartDate(), project.plannedEndDate())
                : texts.dateRange(data.work().plannedStartDate(), data.work().plannedEndDate()) });
        document.facts(facts);
    }

    private void writeSummary(ReportDocument document, ReportSummary summary, ReportTexts texts) {
        document.section(texts.text("summary.title"));
        document.paragraph(summary.summary());
        if (!summary.recommendations().isEmpty()) {
            document.subheading(texts.text("summary.recommendations"));
            document.bullets(summary.recommendations());
        }
        document.note(texts.text("summary.byAi", Map.of("model", summary.model() == null ? "AI" : summary.model())));
    }

    private void writeProgress(ReportDocument document, WorkOverview work, ReportTexts texts) {
        document.section(texts.text("work.title"));
        document.figures(List.of(
                new ReportDocument.Figure(texts.text("work.progress"), texts.percent(work.progressPct()),
                        ReportDocument.BRICK),
                new ReportDocument.Figure(texts.text("work.tasks"), texts.text("work.tasksValue",
                        Map.of("done", work.completedTasks(), "total", work.taskCount()))),
                new ReportDocument.Figure(texts.text("work.phases"), texts.text("work.tasksValue",
                        Map.of("done", work.completedPhases(), "total", work.phaseCount()))),
                new ReportDocument.Figure(texts.text("work.lateTasks"), String.valueOf(work.lateTasks()),
                        work.lateTasks() > 0 ? ReportDocument.DANGER : null)));
        document.note(texts.text("work.currentPhase") + ": "
                + (work.currentPhase() == null ? texts.text("work.none") : work.currentPhase()));
    }

    private void writePhases(ReportDocument document, WorkOverview work, ReportTexts texts) {
        document.section(texts.text("phases.title"));
        if (work.phases().isEmpty()) {
            document.note(texts.text("phases.empty"));
            return;
        }
        ReportDocument.Table table = document.table(new float[] { 29, 14, 22, 25, 10 }, texts.text("phases.name"),
                texts.text("phases.status"), texts.text("phases.progress"), texts.text("phases.dates"),
                texts.text("phases.late"));
        for (PhaseProgress phase : work.phases()) {
            table.text(phase.order() + ". " + phase.name())
                    .colored(status(texts, phase.status()), statusColor(phase.status().code()))
                    .bar(phase.progressPct(), texts.percent(phase.progressPct()))
                    .text(texts.dateRange(phase.plannedStartDate(), phase.plannedEndDate()))
                    .colored(texts.text(phase.late() ? "phases.yes" : "phases.no"),
                            phase.late() ? ReportDocument.DANGER : ReportDocument.MUTED)
                    .end();
        }
        table.close();
    }

    private void writeTasks(ReportDocument document, WorkOverview work, ReportTexts texts) {
        document.section(texts.text("tasks.title"));
        if (work.upcomingTasks().isEmpty()) {
            document.note(texts.text("tasks.empty"));
            return;
        }
        ReportDocument.Table table = document.table(new float[] { 30, 24, 15, 15, 16 }, texts.text("tasks.name"),
                texts.text("tasks.phase"), texts.text("tasks.status"), texts.text("tasks.end"),
                texts.text("tasks.progress"));
        for (UpcomingTask task : work.upcomingTasks()) {
            table.text(task.name())
                    .text(task.phaseName())
                    .colored(status(texts, task.status()), task.late() ? ReportDocument.DANGER
                            : statusColor(task.status().code()))
                    .text(task.plannedEndDate() == null ? null : texts.date(task.plannedEndDate()))
                    .bar(task.progressPct(), texts.percent(task.progressPct()))
                    .end();
        }
        table.close();
    }

    /** Label of a status in the language of the report. */
    protected static String status(ReportTexts texts, StatusResponse status) {
        return texts.code("status", status.code(), status.label());
    }

    /** Color of a status or risk level: green when done or low, red when late or critical. */
    protected static Color statusColor(String code) {
        return switch (code == null ? "" : code) {
            case "completed", "low" -> ReportDocument.GOOD;
            case "in_progress", "medium" -> ReportDocument.BRICK;
            case "delayed", "critical", "cancelled" -> ReportDocument.DANGER;
            case "high", "paused" -> ReportDocument.WARNING;
            default -> ReportDocument.MUTED;
        };
    }

    /** "Casa Los Pinos" -> "casa-los-pinos-public-report.pdf" */
    String filename(String projectName) {
        String plain = Normalizer.normalize(projectName, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.isEmpty()) {
            slug = "project";
        }
        return slug + "-" + type().getCode() + "-report.pdf";
    }

    /** Zero when the value is missing, for figures that are always shown. */
    protected static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
