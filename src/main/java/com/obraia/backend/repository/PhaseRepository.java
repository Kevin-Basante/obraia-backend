package com.obraia.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.obraia.backend.entity.phase.Phase;

public interface PhaseRepository extends JpaRepository<Phase, UUID> {

    List<Phase> findByProjectIdOrderBySequenceOrderAsc(UUID projectId);

    Optional<Phase> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsByProjectId(UUID projectId);

    /** Removes the plan of a project. Tasks and reports go with it (ON DELETE CASCADE). */
    @Modifying
    @Query("DELETE FROM Phase p WHERE p.project.id = :projectId")
    int deleteByProjectId(@Param("projectId") UUID projectId);
}
