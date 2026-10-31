package com.obraia.backend.entity.risk;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;

import com.obraia.backend.entity.project.Project;

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
 * One assessment of the risk of a project. They are never changed: each
 * new expense or progress report adds another one, so the history shows
 * how the risk moved.
 */
@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(name = "risk_level", nullable = false, length = 10, updatable = false)
    private RiskLevel riskLevel;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2, updatable = false)
    private BigDecimal riskScore;

    @Column(name = "predicted_delay_days", nullable = false, updatable = false)
    private int predictedDelayDays;

    /** The factors as JSON text; the column is jsonb, so the text is cast when it is written. */
    @Column(name = "risk_factors", nullable = false, updatable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String riskFactors;

    @Column(name = "model_version", nullable = false, length = 30, updatable = false)
    private String modelVersion;

    @Column(name = "trigger_source", nullable = false, length = 20, updatable = false)
    private RiskTrigger triggerSource;

    @Column(name = "assessed_at", nullable = false, updatable = false)
    private Instant assessedAt;

    protected RiskAssessment() {
        // Required by JPA
    }

    public RiskAssessment(Project project, RiskLevel riskLevel, BigDecimal riskScore, int predictedDelayDays,
            String riskFactors, String modelVersion, RiskTrigger triggerSource, Instant assessedAt) {
        this.project = project;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.predictedDelayDays = predictedDelayDays;
        this.riskFactors = riskFactors;
        this.modelVersion = modelVersion;
        this.triggerSource = triggerSource;
        this.assessedAt = assessedAt;
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public int getPredictedDelayDays() {
        return predictedDelayDays;
    }

    public String getRiskFactors() {
        return riskFactors;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public RiskTrigger getTriggerSource() {
        return triggerSource;
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }
}
