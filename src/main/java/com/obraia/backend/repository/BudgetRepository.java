package com.obraia.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.obraia.backend.entity.budget.Budget;
import com.obraia.backend.entity.budget.BudgetStatus;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    /** Newest version first. */
    List<Budget> findByProjectIdOrderByVersionDesc(UUID projectId);

    Optional<Budget> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<Budget> findByProjectIdAndStatus(UUID projectId, BudgetStatus status);

    /** 0 when the project has no budgets yet. */
    @Query("SELECT COALESCE(MAX(b.version), 0) FROM Budget b WHERE b.project.id = :projectId")
    int findLastVersion(@Param("projectId") UUID projectId);
}
