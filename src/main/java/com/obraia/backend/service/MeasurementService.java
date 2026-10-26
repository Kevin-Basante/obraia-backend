package com.obraia.backend.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.blueprint.MeasurementRequest;
import com.obraia.backend.dto.blueprint.MeasurementResponse;
import com.obraia.backend.entity.blueprint.Blueprint;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementRules;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.FieldValidationException;
import com.obraia.backend.repository.BlueprintMeasurementRepository;
import com.obraia.backend.repository.BlueprintRepository;

import jakarta.persistence.EntityManager;

/**
 * Measurements of the building elements of a project. The user types the
 * manual ones; the AI ones (block 7) can only be read.
 */
@Service
public class MeasurementService {

    /** Construction order, then floor, then the order they were typed. */
    private static final Comparator<BlueprintMeasurement> ORDER = Comparator
            .comparing(BlueprintMeasurement::getElementType)
            .thenComparingInt(BlueprintMeasurement::getFloorNumber);

    private final BlueprintMeasurementRepository measurementRepository;
    private final BlueprintRepository blueprintRepository;
    private final ProjectAccessService accessService;
    private final EntityManager entityManager;

    public MeasurementService(BlueprintMeasurementRepository measurementRepository,
            BlueprintRepository blueprintRepository, ProjectAccessService accessService,
            EntityManager entityManager) {
        this.measurementRepository = measurementRepository;
        this.blueprintRepository = blueprintRepository;
        this.accessService = accessService;
        this.entityManager = entityManager;
    }

    /** Both filters are optional. */
    @Transactional(readOnly = true)
    public List<MeasurementResponse> list(UUID projectId, UUID userId, Integer floor, String element) {
        accessService.requireMembership(projectId, userId);
        ElementType elementType = element == null || element.isBlank() ? null : ElementType.fromCode(element);
        return measurementRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .filter(measurement -> floor == null || measurement.getFloorNumber() == floor)
                .filter(measurement -> elementType == null || measurement.getElementType() == elementType)
                .sorted(ORDER)
                .map(MeasurementResponse::from)
                .toList();
    }

    @Transactional
    public MeasurementResponse create(UUID projectId, UUID userId, MeasurementRequest request) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        BlueprintMeasurement measurement = new BlueprintMeasurement(project, MeasurementSource.MANUAL);
        apply(measurement, project, request);
        measurementRepository.saveAndFlush(measurement);
        // Reads created_at, which the database fills in.
        entityManager.refresh(measurement);
        return MeasurementResponse.from(measurement);
    }

    /** Checks the rules that depend on the element and the project, then copies the data. */
    private void apply(BlueprintMeasurement measurement, Project project, MeasurementRequest request) {
        ElementType element = ElementType.fromCode(request.elementType());
        Map<String, String> errors = new LinkedHashMap<>();
        if (request.floorNumber() > project.getFloors()) {
            errors.put("floorNumber", "the project has only " + project.getFloors() + " floor");
        }
        for (String field : MeasurementRules.missingDimensions(element, request.widthM(), request.heightM())) {
            errors.put(field, "is required for " + element.getLabel().toLowerCase());
        }
        Blueprint blueprint = null;
        if (request.blueprintId() != null) {
            blueprint = blueprintRepository.findByIdAndProjectId(request.blueprintId(), project.getId())
                    .orElse(null);
            if (blueprint == null) {
                errors.put("blueprintId", "the blueprint does not belong to this project");
            }
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        String notes = request.notes() == null || request.notes().isBlank() ? null : request.notes().strip();
        measurement.update(blueprint, element, request.floorNumber(), request.lengthM(), request.widthM(),
                request.heightM(), request.quantity(), notes);
    }
}
