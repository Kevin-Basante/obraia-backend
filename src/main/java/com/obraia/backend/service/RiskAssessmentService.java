package com.obraia.backend.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.risk.RiskAssessmentResponse;
import com.obraia.backend.dto.risk.RiskOverviewResponse;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.risk.RiskAssessment;
import com.obraia.backend.entity.risk.RiskLevel;
import com.obraia.backend.entity.risk.RiskTrigger;
import com.obraia.backend.event.RiskAssessedEvent;
import com.obraia.backend.repository.RiskAssessmentRepository;
import com.obraia.backend.risk.RiskEvaluator;
import com.obraia.backend.risk.RiskFactor;
import com.obraia.backend.risk.RiskResult;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Assesses the risk of a project with the {@link RiskEvaluator} and keeps
 * every result, so the history shows how the risk moved.
 */
@Service
public class RiskAssessmentService {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<List<RiskFactor>> FACTORS = new TypeReference<>() {
    };

    private final RiskAssessmentRepository assessmentRepository;
    private final ProjectSnapshots snapshots;
    private final RiskEvaluator evaluator;
    private final ProjectAccessService accessService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RiskAssessmentService(RiskAssessmentRepository assessmentRepository, ProjectSnapshots snapshots,
            RiskEvaluator evaluator, ProjectAccessService accessService, ApplicationEventPublisher events,
            Clock clock) {
        this.assessmentRepository = assessmentRepository;
        this.snapshots = snapshots;
        this.evaluator = evaluator;
        this.accessService = accessService;
        this.events = events;
        this.clock = clock;
    }

    /** The latest assessment and the history of the score. */
    @Transactional(readOnly = true)
    public RiskOverviewResponse overview(UUID projectId, UUID userId) {
        accessService.requireMembership(projectId, userId);
        List<RiskAssessment> newestFirst = assessmentRepository.findTop20ByProjectIdOrderByAssessedAtDesc(projectId);
        RiskAssessmentResponse latest = newestFirst.isEmpty() ? null : response(newestFirst.get(0));
        List<RiskOverviewResponse.Point> history = new ArrayList<>(
                newestFirst.stream().map(RiskOverviewResponse.Point::from).toList());
        history.sort(Comparator.comparing(RiskOverviewResponse.Point::assessedAt));
        return new RiskOverviewResponse(latest, history);
    }

    /** The latest assessment with its factors, without checking who asks; empty before the first one. */
    @Transactional(readOnly = true)
    public Optional<RiskAssessmentResponse> latestOf(UUID projectId) {
        return assessmentRepository.findFirstByProjectIdOrderByAssessedAtDesc(projectId)
                .map(RiskAssessmentService::response);
    }

    /** A member asks for a new assessment. */
    @Transactional
    public RiskAssessmentResponse assess(UUID projectId, UUID userId) {
        Project project = accessService.requireProject(projectId, userId);
        return response(save(project, RiskTrigger.MANUAL));
    }

    /**
     * A new assessment after something changed in the project. It joins the
     * transaction of the caller, so only code of this package calls it.
     */
    void assessAutomatically(Project project, RiskTrigger trigger) {
        save(project, trigger);
    }

    private RiskAssessment save(Project project, RiskTrigger trigger) {
        UUID projectId = project.getId();
        RiskLevel previous = assessmentRepository.findFirstByProjectIdOrderByAssessedAtDesc(projectId)
                .map(RiskAssessment::getRiskLevel)
                .orElse(null);
        RiskResult result = evaluator.evaluate(snapshots.read(projectId));
        RiskAssessment assessment = assessmentRepository.save(new RiskAssessment(project, result.level(),
                result.score(), result.predictedDelayDays(), JSON.writeValueAsString(result.factors()),
                RiskEvaluator.MODEL_VERSION, trigger, clock.instant()));
        RiskFactor mainFactor = result.factors().stream()
                .max(Comparator.comparing(RiskFactor::score))
                .orElse(null);
        events.publishEvent(new RiskAssessedEvent(projectId, previous, result.level(), result.score(),
                result.predictedDelayDays(), mainFactor));
        return assessment;
    }

    private static RiskAssessmentResponse response(RiskAssessment assessment) {
        return RiskAssessmentResponse.from(assessment, JSON.readValue(assessment.getRiskFactors(), FACTORS));
    }
}
