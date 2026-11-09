package com.obraia.backend.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.obraia.backend.ai.AiServiceClient;
import com.obraia.backend.ai.SummaryRequest;
import com.obraia.backend.config.ClockConfig;
import com.obraia.backend.config.ReportProperties;
import com.obraia.backend.dto.overview.MoneyOverview;
import com.obraia.backend.dto.overview.WorkOverview;
import com.obraia.backend.dto.project.ProjectSummaryResponse;
import com.obraia.backend.dto.risk.RiskAssessmentResponse;
import com.obraia.backend.report.ReportData;
import com.obraia.backend.report.ReportLanguage;
import com.obraia.backend.report.ReportSummary;

/**
 * Asks the AI for the summary of a report. The summary is a plus: when the
 * AI is off, fails or takes too long, the report goes out without it.
 */
@Service
public class ReportSummaryService {

    private static final Logger log = LoggerFactory.getLogger(ReportSummaryService.class);

    private final AiServiceClient aiClient;
    private final ExecutorService executor;
    private final ReportProperties properties;

    public ReportSummaryService(AiServiceClient aiClient, @Qualifier("aiExecutor") ExecutorService executor,
            ReportProperties properties) {
        this.aiClient = aiClient;
        this.executor = executor;
        this.properties = properties;
    }

    /** The summary, or null when there is none. */
    public ReportSummary summarize(ReportData data, ReportLanguage language) {
        if (!aiClient.available()) {
            return null;
        }
        SummaryRequest request = new SummaryRequest(data.project().id(), language.getCode(), data.type().getCode(),
                figures(data));
        CompletableFuture<ReportSummary> call = CompletableFuture.supplyAsync(() -> aiClient.summarize(request),
                executor);
        try {
            return call.get(properties.summaryTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            call.cancel(true);
            log.warn("The AI took more than {} to write the summary; the report goes without it",
                    properties.summaryTimeout());
        } catch (ExecutionException ex) {
            log.warn("The AI could not write the summary: {}", ex.getCause().getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /**
     * The figures the AI reads, with the names obraia-ia expects. Only what
     * the report itself shows is sent, so the public summary never talks
     * about money.
     */
    static Map<String, Object> figures(ReportData data) {
        Map<String, Object> figures = new LinkedHashMap<>();
        figures.put("today", LocalDate.ofInstant(data.generatedAt(), ClockConfig.ZONE).toString());
        figures.put("project", project(data.project(), data.floors()));
        figures.put("work", work(data.work()));
        if (data.money() != null) {
            figures.put("money", money(data.money()));
        }
        if (data.risk() != null) {
            figures.put("risk", risk(data.risk()));
        }
        return figures;
    }

    private static Map<String, Object> project(ProjectSummaryResponse project, int floors) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", project.name());
        values.put("type", project.projectTypeName());
        values.put("city", project.city());
        values.put("status", project.status().code());
        values.put("built_area_m2", project.builtAreaM2());
        values.put("floors", floors);
        values.put("planned_start", text(project.plannedStartDate()));
        values.put("planned_end", text(project.plannedEndDate()));
        return values;
    }

    private static Map<String, Object> work(WorkOverview work) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("progress_pct", work.progressPct());
        values.put("phase_count", work.phaseCount());
        values.put("completed_phases", work.completedPhases());
        values.put("task_count", work.taskCount());
        values.put("completed_tasks", work.completedTasks());
        values.put("late_tasks", work.lateTasks());
        values.put("current_phase", work.currentPhase());
        values.put("phases", work.phases().stream().map(phase -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", phase.name());
            row.put("status", phase.status().code());
            row.put("progress_pct", phase.progressPct());
            row.put("late", phase.late());
            row.put("planned_end", text(phase.plannedEndDate()));
            return row;
        }).toList());
        values.put("upcoming_tasks", work.upcomingTasks().stream().map(task -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", task.name());
            row.put("phase", task.phaseName());
            row.put("status", task.status().code());
            row.put("progress_pct", task.progressPct());
            row.put("late", task.late());
            row.put("planned_end", text(task.plannedEndDate()));
            return row;
        }).toList());
        return values;
    }

    private static Map<String, Object> money(MoneyOverview money) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("approved_budget", money.approvedBudget());
        values.put("total_spent", money.totalSpent());
        values.put("remaining", money.remaining());
        values.put("spent_pct", money.spentPct());
        values.put("budget_versions", money.budgetVersions());
        values.put("expense_count", money.expenseCount());
        values.put("by_category", money.byCategory().stream().map(total -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", total.category().code());
            row.put("amount", total.amount());
            row.put("count", total.count());
            return row;
        }).toList());
        return values;
    }

    private static Map<String, Object> risk(RiskAssessmentResponse risk) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("level", risk.level().code());
        values.put("score", risk.score());
        values.put("predicted_delay_days", risk.predictedDelayDays());
        List<Map<String, Object>> factors = risk.factors().stream().map(factor -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", factor.code());
            row.put("score", factor.score());
            row.put("weight", factor.weight());
            row.put("detail", factor.detail());
            return row;
        }).toList();
        values.put("factors", factors);
        return values;
    }

    private static String text(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
