package com.obraia.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.entity.blueprint.BlueprintMeasurement;

public interface BlueprintMeasurementRepository extends JpaRepository<BlueprintMeasurement, UUID> {

    /** Oldest first; the service sorts them by element and floor. */
    @EntityGraph(attributePaths = "blueprint")
    List<BlueprintMeasurement> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    @EntityGraph(attributePaths = "blueprint")
    Optional<BlueprintMeasurement> findByIdAndProjectId(UUID id, UUID projectId);
}
