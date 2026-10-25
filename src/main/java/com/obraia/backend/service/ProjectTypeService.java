package com.obraia.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.catalog.ProjectTypeResponse;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.ProjectTypeRepository;

/** Project types with their standard phases and material formulas. Read only. */
@Service
public class ProjectTypeService {

    private final ProjectTypeRepository projectTypeRepository;

    public ProjectTypeService(ProjectTypeRepository projectTypeRepository) {
        this.projectTypeRepository = projectTypeRepository;
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

    private static ResourceNotFoundException notFound(UUID id) {
        return new ResourceNotFoundException("PROJECT_TYPE_NOT_FOUND", "The project type " + id + " does not exist");
    }
}
