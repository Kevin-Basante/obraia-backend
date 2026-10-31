package com.obraia.backend.entity.risk;

import com.obraia.backend.exception.BadRequestException;

/** What made the project be assessed again. */
public enum RiskTrigger {

    PROGRESS_REPORT("progress_report", "Progress report"),
    EXPENSE("expense", "Expense"),
    SCHEDULED("scheduled", "Scheduled"),
    MANUAL("manual", "Requested by a member");

    private final String code;
    private final String label;

    RiskTrigger(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static RiskTrigger fromCode(String code) {
        for (RiskTrigger trigger : values()) {
            if (trigger.code.equals(code)) {
                return trigger;
            }
        }
        throw new BadRequestException("INVALID_RISK_TRIGGER", "Unknown risk trigger: " + code);
    }
}
