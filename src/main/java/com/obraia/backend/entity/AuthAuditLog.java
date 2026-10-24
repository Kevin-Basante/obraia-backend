package com.obraia.backend.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One row per security event (sign in, failed attempt, password change...). */
@Entity
@Table(name = "auth_audit_logs")
public class AuthAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Null when someone tries to sign in with an email that does not exist.
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "attempted_email")
    private String attemptedEmail;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected AuthAuditLog() {
        // Required by JPA
    }

    public AuthAuditLog(AuditEvent event, UUID userId, String attemptedEmail, String ipAddress, String userAgent) {
        this.eventType = event.getValue();
        this.userId = userId;
        this.attemptedEmail = attemptedEmail;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getAttemptedEmail() {
        return attemptedEmail;
    }

    public String getEventType() {
        return eventType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
