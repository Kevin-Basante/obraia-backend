package com.obraia.backend.dto.report;

import java.time.Instant;

/**
 * Whether the user can download the full report of a project.
 *
 * @param status        "owner" (the creator, always), "none" (never asked), "pending" (waiting for the
 *                      creator), "approved" (can download) or "expired" (the approval or the link ran out)
 * @param requestedAt   when the latest request was made; null without one
 * @param downloadUntil until when an approved request lets the user download; null when not approved
 */
public record FullReportAccessResponse(String status, Instant requestedAt, Instant downloadUntil) {

    public static final String OWNER = "owner";
    public static final String NONE = "none";
    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";
    public static final String EXPIRED = "expired";

    public boolean canDownload() {
        return OWNER.equals(status) || APPROVED.equals(status);
    }
}
