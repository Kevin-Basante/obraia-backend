package com.obraia.backend.entity.budget;

import com.obraia.backend.exception.BadRequestException;

/**
 * Status of a budget version. A new quotation is a draft; approving it
 * turns the budget approved before it into superseded.
 */
public enum BudgetStatus {

    DRAFT("draft", "Draft"),
    APPROVED("approved", "Approved"),
    SUPERSEDED("superseded", "Superseded");

    private final String code;
    private final String label;

    BudgetStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static BudgetStatus fromCode(String code) {
        for (BudgetStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new BadRequestException("INVALID_BUDGET_STATUS", "Unknown budget status: " + code);
    }
}
