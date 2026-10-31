package com.obraia.backend.entity.risk;

import java.math.BigDecimal;

import com.obraia.backend.exception.BadRequestException;

/** How likely the project is to be late or over budget, from its risk score (0 to 100). */
public enum RiskLevel {

    LOW("low", "Low"),
    MEDIUM("medium", "Medium"),
    HIGH("high", "High"),
    CRITICAL("critical", "Critical");

    private static final BigDecimal MEDIUM_FROM = new BigDecimal("25");
    private static final BigDecimal HIGH_FROM = new BigDecimal("50");
    private static final BigDecimal CRITICAL_FROM = new BigDecimal("75");

    private final String code;
    private final String label;

    RiskLevel(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /** Low below 25, medium below 50, high below 75 and critical from 75. */
    public static RiskLevel fromScore(BigDecimal score) {
        if (score.compareTo(CRITICAL_FROM) >= 0) {
            return CRITICAL;
        }
        if (score.compareTo(HIGH_FROM) >= 0) {
            return HIGH;
        }
        return score.compareTo(MEDIUM_FROM) >= 0 ? MEDIUM : LOW;
    }

    public boolean isAtLeast(RiskLevel other) {
        return compareTo(other) >= 0;
    }

    public static RiskLevel fromCode(String code) {
        for (RiskLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new BadRequestException("INVALID_RISK_LEVEL", "Unknown risk level: " + code);
    }
}
