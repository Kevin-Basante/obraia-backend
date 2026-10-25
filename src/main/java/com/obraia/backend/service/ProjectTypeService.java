package com.obraia.backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.catalog.PhasePlanResponse;
import com.obraia.backend.dto.catalog.PhaseTemplateResponse;
import com.obraia.backend.dto.catalog.ProjectTypeResponse;
import com.obraia.backend.entity.catalog.SizeCategory;
import com.obraia.backend.exception.BadRequestException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.PhaseTemplateRepository;
import com.obraia.backend.repository.ProjectTypeRepository;

/** Project types with their standard phases and material formulas. Read only. */
@Service
public class ProjectTypeService {

    private final ProjectTypeRepository projectTypeRepository;
    private final PhaseTemplateRepository phaseTemplateRepository;

    public ProjectTypeService(ProjectTypeRepository projectTypeRepository,
            PhaseTemplateRepository phaseTemplateRepository) {
        this.projectTypeRepository = projectTypeRepository;
        this.phaseTemplateRepository = phaseTemplateRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectTypeResponse> listProjectTypes() {
        return projectTypeRepository.findAllByOrderByNameAsc().stream().map(ProjectTypeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectTypeResponse getProjectType(UUID id) {
        return listProjectTypes().stream()
                .filter(type -> type.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound(id));
    }

    /**
     * Standard phases for a project type. The size comes from the built
     * area when it is given; otherwise from the size code (small by default).
     */
    @Transactional(readOnly = true)
    public PhasePlanResponse getPhasePlan(UUID projectTypeId, String sizeCode, BigDecimal builtAreaM2) {
        getProjectType(projectTypeId);
        SizeCategory size = resolveSize(sizeCode, builtAreaM2);
        List<PhaseTemplateResponse> phases = phaseTemplateRepository
                .findByProjectTypeIdAndSizeCategoryOrderBySequenceOrderAsc(projectTypeId, size).stream()
                .map(PhaseTemplateResponse::from)
                .toList();
        return PhasePlanResponse.of(projectTypeId, size.getCode(), phases);
    }

    private static SizeCategory resolveSize(String sizeCode, BigDecimal builtAreaM2) {
        if (builtAreaM2 != null) {
            if (builtAreaM2.signum() <= 0) {
                throw new BadRequestException("INVALID_AREA", "The built area must be greater than 0 m2");
            }
            return SizeCategory.forArea(builtAreaM2);
        }
        return sizeCode == null || sizeCode.isBlank() ? SizeCategory.SMALL : SizeCategory.fromCode(sizeCode);
    }

    private static ResourceNotFoundException notFound(UUID id) {
        return new ResourceNotFoundException("PROJECT_TYPE_NOT_FOUND", "The project type " + id + " does not exist");
    }
}
