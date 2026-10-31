package com.obraia.backend.entity.expense;

import com.obraia.backend.exception.BadRequestException;

/** What the money of an expense was spent on. The codes are the ones allowed by the database. */
public enum ExpenseCategory {

    MATERIALS("materials", "Materials"),
    LABOR("labor", "Labor"),
    EQUIPMENT("equipment", "Equipment"),
    TAX("tax", "Taxes"),
    OTHER("other", "Other");

    private final String code;
    private final String label;

    ExpenseCategory(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static ExpenseCategory fromCode(String code) {
        for (ExpenseCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new BadRequestException("INVALID_EXPENSE_CATEGORY", "Unknown expense category: " + code
                + ". Valid values: materials, labor, equipment, tax, other");
    }
}
