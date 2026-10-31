package com.obraia.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.entity.risk.RiskAssessment;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    Optional<RiskAssessment> findFirstByProjectIdOrderByAssessedAtDesc(UUID projectId);

    /** The last 20, newest first. */
    List<RiskAssessment> findTop20ByProjectIdOrderByAssessedAtDesc(UUID projectId);
}
