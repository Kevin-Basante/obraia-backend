package com.obraia.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.entity.catalog.PhaseTemplate;
import com.obraia.backend.entity.catalog.SizeCategory;

public interface PhaseTemplateRepository extends JpaRepository<PhaseTemplate, UUID> {

    List<PhaseTemplate> findByProjectTypeIdAndSizeCategoryOrderBySequenceOrderAsc(UUID projectTypeId,
            SizeCategory sizeCategory);
}
