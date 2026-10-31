package com.obraia.backend.risk;

/**
 * Strategy pattern: one way of measuring the risk of a project. Each
 * implementation looks at one thing (late tasks, progress, money) and the
 * {@link RiskEvaluator} combines all of them. A new way of measuring is a
 * new class; the evaluator does not change.
 */
public interface RiskStrategy {

    RiskFactor evaluate(ProjectSnapshot snapshot);
}
