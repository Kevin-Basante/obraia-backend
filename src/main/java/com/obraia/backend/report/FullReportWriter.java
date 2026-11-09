package com.obraia.backend.report;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.obraia.backend.dto.expense.ExpenseSummaryResponse;
import com.obraia.backend.dto.overview.ActivityCounts;
import com.obraia.backend.dto.overview.MoneyOverview;
import com.obraia.backend.dto.risk.RiskAssessmentResponse;
import com.obraia.backend.entity.risk.RiskLevel;
import com.obraia.backend.risk.RiskFactor;

/** The full report: the public one plus budget, expenses, risk and records. */
@Component
public class FullReportWriter extends ProjectReportWriter {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public ReportType type() {
        return ReportType.FULL;
    }

    @Override
    protected void writeRestrictedSections(ReportDocument document, ReportData data, ReportTexts texts) {
        writeMoney(document, data.money(), texts);
        writeRisk(document, data.risk(), texts);
        writeCounts(document, data.counts(), texts);
    }

    private void writeMoney(ReportDocument document, MoneyOverview money, ReportTexts texts) {
        document.section(texts.text("money.title"));
        if (money.approvedBudget() == null) {
            document.figures(List.of(
                    new ReportDocument.Figure(texts.text("money.approved"), texts.text("money.noBudget")),
                    new ReportDocument.Figure(texts.text("money.spent"), texts.money(money.totalSpent()))));
        } else {
            document.figures(List.of(
                    new ReportDocument.Figure(texts.text("money.approved"), texts.money(money.approvedBudget())),
                    new ReportDocument.Figure(texts.text("money.spent"), texts.money(money.totalSpent()),
                            ReportDocument.BRICK),
                    new ReportDocument.Figure(texts.text("money.remaining"), texts.money(money.remaining()),
                            money.remaining().signum() < 0 ? ReportDocument.DANGER : null),
                    new ReportDocument.Figure(texts.text("money.spentPct"), texts.percent(orZero(money.spentPct())),
                            spentColor(money))));
        }
        StringBuilder line = new StringBuilder();
        if (money.approvedVersion() != null) {
            line.append(texts.text("money.approved")).append(": ").append(texts.text("money.approvedValue",
                    Map.of("amount", texts.money(money.approvedBudget()), "version", money.approvedVersion())))
                    .append(" · ");
        }
        line.append(texts.text("money.versions")).append(": ").append(money.budgetVersions()).append(" · ")
                .append(texts.text("money.expenses")).append(": ").append(money.expenseCount());
        document.note(line.toString());

        document.subheading(texts.text("money.byCategory"));
        if (money.byCategory().isEmpty()) {
            document.note(texts.text("money.noExpenses"));
            return;
        }
        ReportDocument.Table table = document.table(new float[] { 44, 34, 22 }, texts.text("money.category"),
                texts.text("money.amount"), texts.text("money.count"));
        for (ExpenseSummaryResponse.CategoryTotal total : money.byCategory()) {
            table.row(texts.code("expenseCategory", total.category().code(), total.category().label()),
                    texts.money(total.amount()), String.valueOf(total.count()));
        }
        table.close();
    }

    private void writeRisk(ReportDocument document, RiskAssessmentResponse risk, ReportTexts texts) {
        document.section(texts.text("risk.title"));
        if (risk == null) {
            document.note(texts.text("risk.none"));
            return;
        }
        document.figures(List.of(
                new ReportDocument.Figure(texts.text("risk.level"), status(texts, risk.level()),
                        statusColor(risk.level().code())),
                new ReportDocument.Figure(texts.text("risk.score"),
                        texts.text("risk.scoreValue", Map.of("score", texts.number(risk.score(), 1)))),
                new ReportDocument.Figure(texts.text("risk.delay"),
                        texts.text("risk.delayValue", Map.of("count", risk.predictedDelayDays()))),
                new ReportDocument.Figure(texts.text("risk.assessed"), texts.dateTime(risk.assessedAt()))));
        ReportDocument.Table table = document.table(new float[] { 26, 10, 12, 52 }, texts.text("risk.factor"),
                texts.text("risk.weight"), texts.text("risk.factorScore"), texts.text("risk.detail"));
        for (RiskFactor factor : risk.factors()) {
            table.text(texts.code("riskFactor", factor.code(), factor.label()))
                    .text(texts.percent(factor.weight().multiply(HUNDRED)))
                    .colored(texts.number(factor.score(), 1), scoreColor(factor))
                    .text(detail(factor, texts))
                    .end();
        }
        table.close();
    }

    private void writeCounts(ReportDocument document, ActivityCounts counts, ReportTexts texts) {
        document.section(texts.text("counts.title"));
        document.figures(List.of(
                new ReportDocument.Figure(texts.text("counts.blueprints"), String.valueOf(counts.blueprints())),
                new ReportDocument.Figure(texts.text("counts.manual"), String.valueOf(counts.manualMeasurements())),
                new ReportDocument.Figure(texts.text("counts.ai"), String.valueOf(counts.aiMeasurements())),
                new ReportDocument.Figure(texts.text("counts.members"), String.valueOf(counts.members()))));
    }

    /** The sentence of a factor in the language of the report, like the risk page of the frontend. */
    static String detail(RiskFactor factor, ReportTexts texts) {
        String code = factor.detailCode();
        Map<String, String> params = factor.detailParams() == null ? Map.of() : factor.detailParams();
        if ("late_tasks".equals(code)) {
            return texts.text("riskDetail.late_tasks", Map.of("count", params.getOrDefault("count", "0"))) + "; "
                    + texts.text("riskDetail.worst_task", Map.of("task", params.getOrDefault("task", ""),
                            "count", params.getOrDefault("days", "0")));
        }
        return code != null && texts.has("riskDetail." + code) ? texts.text("riskDetail." + code, params)
                : factor.detail();
    }

    private static Color scoreColor(RiskFactor factor) {
        return statusColor(RiskLevel.fromScore(factor.score()).getCode());
    }

    private static Color spentColor(MoneyOverview money) {
        if (money.spentPct() == null) {
            return null;
        }
        return money.spentPct().compareTo(HUNDRED) > 0 ? ReportDocument.DANGER : null;
    }
}
