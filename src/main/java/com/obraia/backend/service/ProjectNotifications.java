package com.obraia.backend.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.obraia.backend.entity.notification.NotificationType;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.risk.RiskLevel;
import com.obraia.backend.event.BudgetApprovedEvent;
import com.obraia.backend.event.BudgetExceededEvent;
import com.obraia.backend.event.RiskAssessedEvent;
import com.obraia.backend.repository.ProjectRepository;

/**
 * Observer: turns what happens in a project into notifications for its
 * members. It runs after the change is committed, in its own transaction, so
 * a failure here never undoes the change.
 */
@Component
public class ProjectNotifications {

    private static final Logger log = LoggerFactory.getLogger(ProjectNotifications.class);

    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;
    private final TransactionTemplate newTransaction;

    public ProjectNotifications(ProjectRepository projectRepository, NotificationService notificationService,
            PlatformTransactionManager transactionManager) {
        this.projectRepository = projectRepository;
        this.notificationService = notificationService;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener
    public void onBudgetApproved(BudgetApprovedEvent event) {
        notify(event.projectId(), NotificationType.BUDGET_APPROVED, project -> "Version " + event.version()
                + " of the budget of \"" + project.getName() + "\" was approved for "
                + money(event.projectedAmount()) + ".");
    }

    @TransactionalEventListener
    public void onBudgetExceeded(BudgetExceededEvent event) {
        notify(event.projectId(), NotificationType.BUDGET_DEVIATION, project -> "The expenses of \""
                + project.getName() + "\" passed the approved budget: " + money(event.spent()) + " spent of "
                + money(event.approvedBudget()) + ".");
    }

    /** Only when the risk reaches high or critical and is worse than before. */
    @TransactionalEventListener
    public void onRiskAssessed(RiskAssessedEvent event) {
        boolean rose = event.previousLevel() == null || event.level().compareTo(event.previousLevel()) > 0;
        if (!event.level().isAtLeast(RiskLevel.HIGH) || !rose) {
            return;
        }
        notify(event.projectId(), NotificationType.DELAY_RISK, project -> {
            StringBuilder message = new StringBuilder("The risk of \"").append(project.getName())
                    .append("\" rose to ").append(event.level().getCode())
                    .append(" (").append(event.score().toPlainString()).append(" of 100)");
            if (event.mainReason() != null) {
                message.append(": ").append(event.mainReason());
            }
            message.append('.');
            if (event.predictedDelayDays() > 0) {
                message.append(" About ").append(event.predictedDelayDays()).append(" days of delay are expected.");
            }
            return message.toString();
        });
    }

    private void notify(UUID projectId, NotificationType type, Function<Project, String> message) {
        try {
            newTransaction.executeWithoutResult(status -> projectRepository.findByIdAndDeletedAtIsNull(projectId)
                    .ifPresent(project -> notificationService.notifyMembers(projectId, type, message.apply(project))));
        } catch (RuntimeException ex) {
            log.error("Could not send the {} notification of the project {}", type.getCode(), projectId, ex);
        }
    }

    /** Colombian pesos without cents: "COP 120,000,000". */
    static String money(BigDecimal amount) {
        return String.format(Locale.US, "COP %,.0f", amount);
    }
}
