package com.obraia.backend.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.obraia.backend.entity.risk.RiskTrigger;
import com.obraia.backend.event.ExpensesChangedEvent;
import com.obraia.backend.event.ProgressReportedEvent;
import com.obraia.backend.repository.ProjectRepository;

/**
 * Observer: assesses the risk again when an expense or a progress report is
 * saved. It runs after the change is committed, in its own transaction, so a
 * failure here never undoes the expense or the report.
 */
@Component
public class RiskTriggers {

    private static final Logger log = LoggerFactory.getLogger(RiskTriggers.class);

    private final ProjectRepository projectRepository;
    private final RiskAssessmentService riskService;
    private final TransactionTemplate newTransaction;

    public RiskTriggers(ProjectRepository projectRepository, RiskAssessmentService riskService,
            PlatformTransactionManager transactionManager) {
        this.projectRepository = projectRepository;
        this.riskService = riskService;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener
    public void onExpensesChanged(ExpensesChangedEvent event) {
        assess(event.projectId(), RiskTrigger.EXPENSE);
    }

    @TransactionalEventListener
    public void onProgressReported(ProgressReportedEvent event) {
        assess(event.projectId(), RiskTrigger.PROGRESS_REPORT);
    }

    private void assess(UUID projectId, RiskTrigger trigger) {
        try {
            newTransaction.executeWithoutResult(status -> projectRepository.findByIdAndDeletedAtIsNull(projectId)
                    .filter(project -> !project.getStatus().nextStatuses().isEmpty())
                    .ifPresent(project -> riskService.assessAutomatically(project, trigger)));
        } catch (RuntimeException ex) {
            log.error("Could not assess the risk of the project {} after a {}", projectId, trigger.getCode(), ex);
        }
    }
}
