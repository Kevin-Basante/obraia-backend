package com.obraia.backend.entity.phase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.obraia.backend.entity.User;
import com.obraia.backend.entity.project.SiteManager;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * How far a task went on a given day. It is saved by a member of the
 * project; siteManager says when the site manager gave the information.
 */
@Entity
@Table(name = "progress_reports")
public class ProgressReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, updatable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by", updatable = false)
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_manager_id", updatable = false)
    private SiteManager siteManager;

    @Column(name = "completion_percentage", nullable = false, precision = 5, scale = 2, updatable = false)
    private BigDecimal completionPercentage;

    @Column(name = "notes", updatable = false)
    private String notes;

    @Column(name = "report_date", nullable = false, updatable = false)
    private LocalDate reportDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected ProgressReport() {
        // Required by JPA
    }

    public ProgressReport(Task task, User reportedBy, SiteManager siteManager, BigDecimal completionPercentage,
            String notes, LocalDate reportDate) {
        this.task = task;
        this.reportedBy = reportedBy;
        this.siteManager = siteManager;
        this.completionPercentage = completionPercentage;
        this.notes = notes;
        this.reportDate = reportDate;
    }

    public UUID getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public User getReportedBy() {
        return reportedBy;
    }

    public SiteManager getSiteManager() {
        return siteManager;
    }

    public BigDecimal getCompletionPercentage() {
        return completionPercentage;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
