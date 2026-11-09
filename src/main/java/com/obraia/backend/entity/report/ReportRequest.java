package com.obraia.backend.entity.report;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A member asks to download the full report of a project. The creator of
 * the project approves it with the email link; then the member can
 * download it until {@code downloadUntil}. Only the SHA-256 hash of the
 * link token is stored.
 */
@Entity
@Table(name = "report_requests")
public class ReportRequest {

    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID requestedBy;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 20)
    private String status = PENDING;

    @Column(name = "link_expires_at", nullable = false)
    private Instant linkExpiresAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "download_until")
    private Instant downloadUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReportRequest() {
        // Required by JPA
    }

    public ReportRequest(UUID projectId, UUID requestedBy, String tokenHash, Instant linkExpiresAt,
            Instant createdAt) {
        this.projectId = projectId;
        this.requestedBy = requestedBy;
        this.tokenHash = tokenHash;
        this.linkExpiresAt = linkExpiresAt;
        this.createdAt = createdAt;
    }

    /** The email link works once and only before it expires. */
    public boolean canBeApproved(Instant now) {
        return PENDING.equals(status) && now.isBefore(linkExpiresAt);
    }

    public void approve(Instant now, Duration downloadWindow) {
        this.status = APPROVED;
        this.approvedAt = now;
        this.downloadUntil = now.plus(downloadWindow);
    }

    /** True while the member can download the full report. */
    public boolean allowsDownload(Instant now) {
        return APPROVED.equals(status) && now.isBefore(downloadUntil);
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getStatus() {
        return status;
    }

    public Instant getLinkExpiresAt() {
        return linkExpiresAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getDownloadUntil() {
        return downloadUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
