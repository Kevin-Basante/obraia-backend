package com.obraia.backend.entity.project;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Deletion waiting for the email confirmation. Only the SHA-256 hash of
 * the link token is stored. The table also supports account deletion
 * (block 9), so the target is a type plus an id.
 */
@Entity
@Table(name = "deletion_requests")
public class DeletionRequest {

    public static final String TARGET_PROJECT = "project";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "target_type", nullable = false, length = 10, updatable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected DeletionRequest() {
        // Required by JPA
    }

    public DeletionRequest(UUID userId, String targetType, UUID targetId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    /** A link works once and only before it expires. */
    public boolean isUsable(Instant now) {
        return confirmedAt == null && now.isBefore(expiresAt);
    }

    public void markConfirmed(Instant when) {
        this.confirmedAt = when;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }
}
