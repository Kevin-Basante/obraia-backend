package com.obraia.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.obraia.backend.entity.budget.ProjectMaterial;

public interface ProjectMaterialRepository extends JpaRepository<ProjectMaterial, UUID> {

    /** Loads each row with its material in the same query. */
    @EntityGraph(attributePaths = "material")
    List<ProjectMaterial> findByProjectIdOrderByMaterialNameAsc(UUID projectId);

    /** Clears the list before a new quotation writes it again. */
    @Modifying
    @Query("DELETE FROM ProjectMaterial m WHERE m.project.id = :projectId")
    int deleteByProjectId(@Param("projectId") UUID projectId);
}
