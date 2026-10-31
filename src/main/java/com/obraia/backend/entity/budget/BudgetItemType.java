package com.obraia.backend.entity.budget;

import com.obraia.backend.exception.BadRequestException;

/** Kind of cost a budget row holds. Each layer of the quotation is one row. */
public enum BudgetItemType {

    MATERIALS("materials", "Materials"),
    WASTE("waste", "Waste"),
    TRANSPORT("transport", "Transport"),
    LABOR("labor", "Labor"),
    EQUIPMENT("equipment", "Equipment"),
    TAX("tax", "Tax"),
    CONTINGENCY("contingency", "Contingency"),
    OTHER("other", "Other");

    private final String code;
    private final String label;

    BudgetItemType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static BudgetItemType fromCode(String code) {
        for (BudgetItemType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new BadRequestException("INVALID_BUDGET_ITEM_TYPE", "Unknown budget item type: " + code);
    }
}
