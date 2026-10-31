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
import com.obraia.backend.event.BudgetApprovedEvent;
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
