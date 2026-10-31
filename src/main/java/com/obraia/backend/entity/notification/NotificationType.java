package com.obraia.backend.entity.notification;

/** Why a notification was sent. The codes are the ones allowed by the database. */
public enum NotificationType {

    BUDGET_DEVIATION("budget_deviation", "Budget passed"),
    DELAY_RISK("delay_risk", "Risk of delay"),
    PHASE_STATUS_CHANGED("phase_status_changed", "Phase status changed"),
    TASK_OVERDUE("task_overdue", "Task overdue"),
    BUDGET_APPROVED("budget_approved", "Budget approved"),
    DELETION_SCHEDULED("deletion_scheduled", "Deletion scheduled"),
    SYSTEM("system", "System");

    private final String code;
    private final String label;

    NotificationType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static NotificationType fromCode(String code) {
        for (NotificationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown notification type: " + code);
    }
}
