package com.obraia.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.entity.phase.Task;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    /** Every task of the project, in the order they were created. */
    List<Task> findByPhaseProjectIdOrderByCreatedAtAsc(UUID projectId);

    Optional<Task> findByIdAndPhaseProjectId(UUID id, UUID projectId);

    List<Task> findByPhaseId(UUID phaseId);
}
