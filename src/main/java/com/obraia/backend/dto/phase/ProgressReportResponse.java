package com.obraia.backend.dto.phase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.obraia.backend.entity.User;
import com.obraia.backend.entity.phase.ProgressReport;

/** A progress report, with who saved it and the site manager who gave it, if any. */
public record ProgressReportResponse(UUID id, UUID taskId, BigDecimal completionPercentage, String notes,
        LocalDate reportDate, String reportedBy, String siteManager, Instant createdAt) {

    public static ProgressReportResponse from(ProgressReport report) {
        User user = report.getReportedBy();
        return new ProgressReportResponse(report.getId(), report.getTask().getId(), report.getCompletionPercentage(),
                report.getNotes(), report.getReportDate(),
                user == null ? null : user.getFirstName() + " " + user.getLastName(),
                report.getSiteManager() == null ? null : report.getSiteManager().getFullName(),
                report.getCreatedAt());
    }
}
