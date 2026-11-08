package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import com.obraia.backend.entity.notification.NotificationType;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.risk.RiskLevel;
import com.obraia.backend.event.BudgetApprovedEvent;
import com.obraia.backend.event.BudgetExceededEvent;
import com.obraia.backend.event.RiskAssessedEvent;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.risk.RiskFactor;
import com.obraia.backend.support.ProjectFixtures;

class ProjectNotificationsTest {

    private final Project project = ProjectFixtures.project(UUID.randomUUID(), "Casa Avisos");

    private NotificationService notificationService;
    private ProjectNotifications notifications;

    @BeforeEach
    void setUp() {
        ProjectRepository projects = mock(ProjectRepository.class);
        notificationService = mock(NotificationService.class);
        when(projects.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        notifications = new ProjectNotifications(projects, notificationService,
                mock(PlatformTransactionManager.class));
    }

    private RiskAssessedEvent risk(RiskLevel previous, RiskLevel level, int delayDays) {
        RiskFactor reason = new RiskFactor("spending_pace", "Spending against progress", new BigDecimal("100.00"),
                new BigDecimal("0.15"), 0, "110% of the budget is spent with 40% of the work done",
                "spending_vs_progress", Map.of("spent", "110"));
        return new RiskAssessedEvent(project.getId(), previous, level, new BigDecimal("66.88"), delayDays, reason);
    }

    @Test
    void anApprovedBudgetIsNotified() {
        notifications.onBudgetApproved(new BudgetApprovedEvent(project.getId(), 2, new BigDecimal("120000000.00")));

        verify(notificationService).notifyMembers(project.getId(), NotificationType.BUDGET_APPROVED,
                "Version 2 of the budget of \"Casa Avisos\" was approved for COP 120,000,000.",
                Map.of("project", "Casa Avisos", "version", "2", "amount", "120000000"));
    }

    @Test
    void passingTheBudgetIsNotified() {
        notifications.onBudgetExceeded(new BudgetExceededEvent(project.getId(), new BigDecimal("5000000"),
                new BigDecimal("5200000")));

        verify(notificationService).notifyMembers(project.getId(), NotificationType.BUDGET_DEVIATION,
                "The expenses of \"Casa Avisos\" passed the approved budget: COP 5,200,000 spent of COP 5,000,000.",
                Map.of("project", "Casa Avisos", "spent", "5200000", "budget", "5000000"));
    }

    @Test
    void aRiskThatRisesToHighIsNotified() {
        notifications.onRiskAssessed(risk(RiskLevel.MEDIUM, RiskLevel.HIGH, 12));

        verify(notificationService).notifyMembers(project.getId(), NotificationType.DELAY_RISK,
                "The risk of \"Casa Avisos\" rose to high (66.88 of 100): 110% of the budget is spent with 40% of "
                        + "the work done. About 12 days of delay are expected.",
                Map.of("project", "Casa Avisos", "level", "high", "score", "66.88", "delayDays", "12",
                        "reason", "spending_vs_progress", "reason.spent", "110"));
    }

    @Test
    void theFirstAssessmentCountsAsARise() {
        notifications.onRiskAssessed(risk(null, RiskLevel.CRITICAL, 0));

        verify(notificationService).notifyMembers(any(), any(), any(), any());
    }

    @Test
    void lowRisksAndRisksThatDidNotRiseAreNotNotified() {
        notifications.onRiskAssessed(risk(null, RiskLevel.MEDIUM, 0));
        notifications.onRiskAssessed(risk(RiskLevel.HIGH, RiskLevel.HIGH, 0));
        notifications.onRiskAssessed(risk(RiskLevel.CRITICAL, RiskLevel.HIGH, 0));

        verify(notificationService, never()).notifyMembers(any(), any(), any(), any());
    }

    @Test
    void aRiskWithoutFactorsHasNoReason() {
        notifications.onRiskAssessed(new RiskAssessedEvent(project.getId(), null, RiskLevel.HIGH,
                new BigDecimal("70.00"), 0, null));

        verify(notificationService).notifyMembers(project.getId(), NotificationType.DELAY_RISK,
                "The risk of \"Casa Avisos\" rose to high (70.00 of 100).",
                Map.of("project", "Casa Avisos", "level", "high", "score", "70.00", "delayDays", "0"));
    }

    @Test
    void moneyHasNoCents() {
        assertEquals("COP 1,234,568", ProjectNotifications.money(new BigDecimal("1234567.89")));
        assertEquals("1234568", ProjectNotifications.amount(new BigDecimal("1234567.89")));
    }
}
