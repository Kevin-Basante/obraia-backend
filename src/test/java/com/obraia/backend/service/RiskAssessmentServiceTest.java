package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.obraia.backend.dto.risk.RiskAssessmentResponse;
import com.obraia.backend.dto.risk.RiskOverviewResponse;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.risk.RiskAssessment;
import com.obraia.backend.entity.risk.RiskLevel;
import com.obraia.backend.entity.risk.RiskTrigger;
import com.obraia.backend.event.RiskAssessedEvent;
import com.obraia.backend.repository.RiskAssessmentRepository;
import com.obraia.backend.risk.BudgetOverrunStrategy;
import com.obraia.backend.risk.ProgressPaceStrategy;
import com.obraia.backend.risk.ProjectSnapshot;
import com.obraia.backend.risk.RiskEvaluator;
import com.obraia.backend.risk.ScheduleDelayStrategy;
import com.obraia.backend.risk.SpendingPaceStrategy;
import com.obraia.backend.support.ProjectFixtures;

class RiskAssessmentServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"),
            ZoneId.of("America/Bogota"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    private final UUID userId = UUID.randomUUID();
    private final Project project = ProjectFixtures.project(userId, "Casa Riesgo");

    private RiskAssessmentRepository assessments;
    private ProjectSnapshots snapshots;
    private ProjectAccessService access;
    private ApplicationEventPublisher events;
    private RiskAssessmentService service;

    @BeforeEach
    void setUp() {
        assessments = mock(RiskAssessmentRepository.class);
        snapshots = mock(ProjectSnapshots.class);
        access = mock(ProjectAccessService.class);
        events = mock(ApplicationEventPublisher.class);
        RiskEvaluator evaluator = new RiskEvaluator(List.of(new ScheduleDelayStrategy(), new ProgressPaceStrategy(),
                new BudgetOverrunStrategy(), new SpendingPaceStrategy()));
        when(access.requireProject(project.getId(), userId)).thenReturn(project);
        when(assessments.save(any())).thenAnswer(call -> call.getArgument(0));
        when(assessments.findFirstByProjectIdOrderByAssessedAtDesc(project.getId())).thenReturn(Optional.empty());
        // 110% of the budget spent: only the money is wrong.
        when(snapshots.read(project.getId())).thenReturn(new ProjectSnapshot(TODAY, null, null,
                new BigDecimal("40"), List.of(), new BigDecimal("1000000"), new BigDecimal("1100000")));
        service = new RiskAssessmentService(assessments, snapshots, evaluator, access, events, CLOCK);
    }

    @Test
    void assessSavesTheResultWithItsFactors() {
        RiskAssessmentResponse response = service.assess(project.getId(), userId);

        ArgumentCaptor<RiskAssessment> saved = ArgumentCaptor.forClass(RiskAssessment.class);
        verify(assessments).save(saved.capture());
        assertEquals(RiskTrigger.MANUAL, saved.getValue().getTriggerSource());
        assertEquals("rules-v1", saved.getValue().getModelVersion());
        assertEquals(CLOCK.instant(), saved.getValue().getAssessedAt());
        assertTrue(saved.getValue().getRiskFactors().contains("\"code\":\"budget_overrun\""));
        // budget 75, spending pace 100: average 33.75; half of it plus half of 100
        assertEquals(new BigDecimal("66.88"), response.score());
        assertEquals("high", response.level().code());
        assertEquals(4, response.factors().size());
        assertEquals("110% of the approved budget is spent", response.factors().get(2).detail());
    }

    @Test
    void assessAnnouncesTheNewLevelAndTheMainReason() {
        when(assessments.findFirstByProjectIdOrderByAssessedAtDesc(project.getId())).thenReturn(Optional.of(
                new RiskAssessment(project, RiskLevel.LOW, BigDecimal.ZERO, 0, "[]", "rules-v1", RiskTrigger.EXPENSE,
                        CLOCK.instant())));

        service.assess(project.getId(), userId);

        verify(events).publishEvent(new RiskAssessedEvent(project.getId(), RiskLevel.LOW, RiskLevel.HIGH,
                new BigDecimal("66.88"), 0, "110% of the budget is spent with 40% of the work done"));
    }

    @Test
    void overviewHasTheLatestAndTheHistoryOldestFirst() {
        RiskAssessment older = new RiskAssessment(project, RiskLevel.LOW, new BigDecimal("10.00"), 0, "[]",
                "rules-v1", RiskTrigger.EXPENSE, Instant.parse("2026-11-18T15:00:00Z"));
        RiskAssessment newer = new RiskAssessment(project, RiskLevel.MEDIUM, new BigDecimal("30.00"), 4, "[]",
                "rules-v1", RiskTrigger.PROGRESS_REPORT, Instant.parse("2026-11-19T15:00:00Z"));
        when(assessments.findTop20ByProjectIdOrderByAssessedAtDesc(project.getId()))
                .thenReturn(List.of(newer, older));

        RiskOverviewResponse overview = service.overview(project.getId(), userId);

        verify(access).requireMembership(project.getId(), userId);
        assertEquals("medium", overview.latest().level().code());
        assertEquals("progress_report", overview.latest().trigger().code());
        assertEquals(List.of("low", "medium"), overview.history().stream().map(RiskOverviewResponse.Point::level)
                .toList());
    }

    @Test
    void overviewBeforeTheFirstAssessment() {
        when(assessments.findTop20ByProjectIdOrderByAssessedAtDesc(project.getId())).thenReturn(List.of());

        RiskOverviewResponse overview = service.overview(project.getId(), userId);

        assertNull(overview.latest());
        assertEquals(List.of(), overview.history());
    }
}
