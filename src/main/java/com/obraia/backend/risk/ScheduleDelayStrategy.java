package com.obraia.backend.risk;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Tasks past their planned end. The more tasks and the later the worst
 * one, the higher the score; the worst one also gives the predicted delay.
 */
@Component
@Order(1)
public class ScheduleDelayStrategy implements RiskStrategy {

    static final String CODE = "schedule_delay";
    static final BigDecimal WEIGHT = new BigDecimal("0.35");
    private static final String LABEL = "Late tasks";

    @Override
    public RiskFactor evaluate(ProjectSnapshot snapshot) {
        if (snapshot.lateWork().isEmpty()) {
            return new RiskFactor(CODE, LABEL, RiskScores.ZERO, WEIGHT, 0, "No task is past its planned end",
                    "no_late_tasks", Map.of());
        }
        ProjectSnapshot.LateWork worst = snapshot.lateWork().stream()
                .max(Comparator.comparingInt(ProjectSnapshot.LateWork::daysLate))
                .orElseThrow();
        int count = snapshot.lateWork().size();
        // 4 points per day of the worst task and 8 per late task.
        BigDecimal score = RiskScores.clamp(BigDecimal.valueOf(worst.daysLate() * 4L + count * 8L));
        String detail = (count == 1 ? "1 task is" : count + " tasks are") + " past their planned end; \""
                + worst.name() + "\" is " + worst.daysLate() + (worst.daysLate() == 1 ? " day" : " days") + " late";
        Map<String, String> params = Map.of("count", String.valueOf(count), "task", worst.name(),
                "days", String.valueOf(worst.daysLate()));
        return new RiskFactor(CODE, LABEL, score, WEIGHT, worst.daysLate(), detail, "late_tasks", params);
    }
}
