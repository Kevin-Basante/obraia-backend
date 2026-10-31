package com.obraia.backend.entity.blueprint;

/** Progress of the AI reading of a blueprint. Every upload starts as pending. */
public enum ProcessingStatus {

    PENDING("pending", "Pending"),
    PROCESSING("processing", "Processing"),
    COMPLETED("completed", "Completed"),
    FAILED("failed", "Failed");

    private final String code;
    private final String label;

    ProcessingStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static ProcessingStatus fromCode(String code) {
        for (ProcessingStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown processing status: " + code);
    }
}
