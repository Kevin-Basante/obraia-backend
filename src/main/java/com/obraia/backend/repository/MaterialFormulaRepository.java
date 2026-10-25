package com.obraia.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.config.CacheConfig;
import com.obraia.backend.entity.catalog.MaterialFormula;

public interface MaterialFormulaRepository extends JpaRepository<MaterialFormula, UUID> {

    /** Loads each formula with its material in the same query. */
    @Cacheable(CacheConfig.MATERIAL_FORMULAS)
    @EntityGraph(attributePaths = "material")
    List<MaterialFormula> findByProjectTypeId(UUID projectTypeId);
}
