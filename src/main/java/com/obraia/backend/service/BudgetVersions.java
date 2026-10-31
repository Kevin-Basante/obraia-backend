package com.obraia.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import com.obraia.backend.config.CacheConfig;
import com.obraia.backend.dto.budget.BudgetSummaryResponse;
import com.obraia.backend.event.BudgetChangedEvent;
import com.obraia.backend.repository.BudgetRepository;

/**
 * The versions of the budget of each project, kept in the cache. The
 * budget page reads them often and they only change when someone quotes
 * or approves. It is a bean of its own because Spring only applies the
 * cache to calls that come from outside the bean.
 */
@Component
public class BudgetVersions {

    private final BudgetRepository budgetRepository;

    public BudgetVersions(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Cacheable(cacheNames = CacheConfig.BUDGET_VERSIONS, key = "#projectId")
    @Transactional(readOnly = true)
    public List<BudgetSummaryResponse> list(UUID projectId) {
        return budgetRepository.findByProjectIdOrderByVersionDesc(projectId).stream()
                .map(BudgetSummaryResponse::from).toList();
    }

    /**
     * Removes the versions of the project from the cache. It runs after
     * the change is committed, so the next read gets the new data.
     */
    @TransactionalEventListener
    @CacheEvict(cacheNames = CacheConfig.BUDGET_VERSIONS, key = "#event.projectId()")
    public void onBudgetChanged(BudgetChangedEvent event) {
        // The annotations do the work.
    }
}
