package com.obraia.backend.entity;

/** Security events allowed by the CHECK constraint of auth_audit_logs. */
public enum AuditEvent {
    REGISTER("register"),
    EMAIL_VERIFIED("email_verified"),
    LOGIN_SUCCESS("login_success"),
    LOGIN_FAILED("login_failed"),
    LOGOUT("logout"),
    TOKEN_REFRESHED("token_refreshed"),
    PASSWORD_RESET_REQUESTED("password_reset_requested"),
    PASSWORD_CHANGED("password_changed");

    private final String value;

    AuditEvent(String value) {
        this.value = value;
    }

    /** Value stored in the event_type column. */
    public String getValue() {
        return value;
    }
}
