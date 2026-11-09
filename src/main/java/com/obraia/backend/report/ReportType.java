package com.obraia.backend.report;

import com.obraia.backend.exception.BadRequestException;

/** The two reports of a project. */
public enum ReportType {

    /** Progress, phases and tasks; any member can download it. */
    PUBLIC("public"),
    /** The public report plus budget, expenses and risk; it needs the approval of the creator. */
    FULL("full");

    private final String code;

    ReportType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ReportType fromCode(String code) {
        for (ReportType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new BadRequestException("INVALID_REPORT_TYPE", "The report type must be public or full");
    }
}
