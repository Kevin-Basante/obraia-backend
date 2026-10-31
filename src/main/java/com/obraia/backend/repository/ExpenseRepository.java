package com.obraia.backend.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.obraia.backend.entity.expense.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    /** Newest first: by the date of the expense, then by the moment it was saved. */
    @EntityGraph(attributePaths = {"phase", "registeredBy"})
    List<Expense> findByProjectIdOrderByExpenseDateDescCreatedAtDesc(UUID projectId);

    Optional<Expense> findByIdAndProjectId(UUID id, UUID projectId);

    /** Everything spent on the project; 0 without expenses. */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.project.id = :projectId")
    BigDecimal sumByProjectId(@Param("projectId") UUID projectId);
}
