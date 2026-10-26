package com.obraia.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.entity.blueprint.Blueprint;

public interface BlueprintRepository extends JpaRepository<Blueprint, UUID> {

    List<Blueprint> findByProjectIdOrderByUploadedAtDesc(UUID projectId);

    Optional<Blueprint> findByIdAndProjectId(UUID id, UUID projectId);

    long countByProjectId(UUID projectId);
}
