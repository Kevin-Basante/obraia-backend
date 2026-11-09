package com.obraia.backend.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.obraia.backend.dto.expense.ExpenseSummaryResponse;
import com.obraia.backend.dto.overview.ActivityCounts;
import com.obraia.backend.dto.overview.MoneyOverview;
import com.obraia.backend.dto.overview.PhaseProgress;
import com.obraia.backend.dto.overview.UpcomingTask;
import com.obraia.backend.dto.overview.WorkOverview;
import com.obraia.backend.dto.project.ProjectSummaryResponse;
import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.dto.risk.RiskAssessmentResponse;
import com.obraia.backend.report.ReportData;
import com.obraia.backend.report.ReportSummary;
import com.obraia.backend.report.ReportType;
import com.obraia.backend.risk.RiskFactor;

/** The figures of a report for unit tests: a house with late tasks and a budget already passed. */
public final class ReportFixtures {

    public static final Instant GENERATED_AT = Instant.parse("2026-11-09T15:30:00Z");

    private ReportFixtures() {
    }

    public static ProjectSummaryResponse project() {
        return new ProjectSummaryResponse(UUID.randomUUID(), "Casa Basante", "Vivienda unifamiliar", "Pasto",
                new BigDecimal("96.00"), "small", new StatusResponse("in_progress", "In progress"), null, null,
                Instant.parse("2026-10-20T15:00:00Z"));
    }

    public static WorkOverview work() {
        StatusResponse done = new StatusResponse("completed", "Completed");
        StatusResponse moving = new StatusResponse("in_progress", "In progress");
        List<PhaseProgress> phases = List.of(
                new PhaseProgress(UUID.randomUUID(), "Preliminares", 1, done, new BigDecimal("100.00"), false,
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), 3),
                new PhaseProgress(UUID.randomUUID(), "Cimentación", 2, moving, new BigDecimal("70.00"), true,
                        LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 30), 4));
        List<UpcomingTask> tasks = List.of(new UpcomingTask(UUID.randomUUID(), "Vigas de cimentación",
                "Cimentación", moving, LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 30), new BigDecimal("70.00"),
                true));
        return new WorkOverview(new BigDecimal("36.40"), LocalDate.of(2026, 9, 1), LocalDate.of(2027, 1, 30), 2, 1,
                7, 3, 1, "Cimentación", phases, tasks);
    }

    public static MoneyOverview money() {
        List<ExpenseSummaryResponse.CategoryTotal> byCategory = List.of(new ExpenseSummaryResponse.CategoryTotal(
                new StatusResponse("materials", "Materials"), new BigDecimal("80000000.00"), 3));
        return new MoneyOverview(3, 3, new BigDecimal("68645101.00"), new BigDecimal("80000000.00"),
                new BigDecimal("-11354899.00"), new BigDecimal("116.54"), 3, byCategory);
    }

    public static RiskAssessmentResponse risk() {
        List<RiskFactor> factors = List.of(
                new RiskFactor("schedule_delay", "Late tasks", new BigDecimal("100.00"), new BigDecimal("0.35"), 38,
                        "1 task is past their planned end; \"Vigas\" is 38 days late", "late_tasks",
                        Map.of("count", "1", "task", "Vigas", "days", "38")),
                new RiskFactor("budget_overrun", "Spending against budget", new BigDecimal("20.00"),
                        new BigDecimal("0.25"), 0, "117% of the approved budget is spent", "budget_spent",
                        Map.of("spent", "117")));
        return new RiskAssessmentResponse(UUID.randomUUID(), new StatusResponse("critical", "Critical"),
                new BigDecimal("89.50"), 38, factors, "rules-v1", new StatusResponse("manual", "Manual"),
                Instant.parse("2026-11-07T23:45:00Z"));
    }

    public static ReportData publicData() {
        return new ReportData(ReportType.PUBLIC, project(), 2, work(), null, null, null, GENERATED_AT, null);
    }

    public static ReportData fullData() {
        return new ReportData(ReportType.FULL, project(), 2, work(), money(), risk(),
                new ActivityCounts(1, 8, 4, 2), GENERATED_AT, null);
    }

    public static ReportSummary summary() {
        return new ReportSummary("La obra va en el 36,4%.", List.of("Reprogramar las tareas atrasadas."),
                "gemini-test");
    }
}
