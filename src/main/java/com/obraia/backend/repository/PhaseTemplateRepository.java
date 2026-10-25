package com.obraia.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.config.CacheConfig;
import com.obraia.backend.entity.catalog.PhaseTemplate;
import com.obraia.backend.entity.catalog.SizeCategory;

public interface PhaseTemplateRepository extends JpaRepository<PhaseTemplate, UUID> {

    @Cacheable(CacheConfig.PHASE_TEMPLATES)
    List<PhaseTemplate> findByProjectTypeIdAndSizeCategoryOrderBySequenceOrderAsc(UUID projectTypeId,
            SizeCategory sizeCategory);
}
