package com.obraia.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.ai.AiServiceClient;
import com.obraia.backend.ai.AiServiceException;
import com.obraia.backend.ai.AnalysisRequest;
import com.obraia.backend.ai.BlueprintAnalysis;
import com.obraia.backend.dto.blueprint.BlueprintAnalysisResponse;
import com.obraia.backend.dto.blueprint.MeasurementResponse;
import com.obraia.backend.entity.blueprint.Blueprint;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.repository.BlueprintMeasurementRepository;
import com.obraia.backend.storage.BlueprintStorage;

import jakarta.persistence.EntityManager;

/**
 * Reads a blueprint with the AI and saves what it found as measurements
 * with source "ocr". The AI is reached through {@link AiServiceClient},
 * which is the proxy: this service never knows if the answer came from
 * the cache or from a real call.
 */
@Service
public class BlueprintAnalysisService {

    private final BlueprintService blueprintService;
    private final BlueprintMeasurementRepository measurementRepository;
    private final BlueprintStorage storage;
    private final AiServiceClient aiService;
    private final EntityManager entityManager;

    public BlueprintAnalysisService(BlueprintService blueprintService,
            BlueprintMeasurementRepository measurementRepository, BlueprintStorage storage,
            AiServiceClient aiService, EntityManager entityManager) {
        this.blueprintService = blueprintService;
        this.measurementRepository = measurementRepository;
        this.storage = storage;
        this.aiService = aiService;
        this.entityManager = entityManager;
    }

    /**
     * Analyzing again replaces the measurements the AI read before from
     * the same blueprint. If the AI fails, the blueprint is left as failed
     * (that change is kept) and it can be analyzed again.
     */
    @Transactional(noRollbackFor = AiServiceException.class)
    public BlueprintAnalysisResponse analyze(UUID projectId, UUID userId, UUID blueprintId) {
        Blueprint blueprint = blueprintService.requireBlueprint(projectId, userId, blueprintId);
        Project project = blueprint.getProject();
        ProjectService.requireEditable(project);
        BlueprintAnalysis analysis;
        try {
            analysis = aiService.analyze(new AnalysisRequest(projectId, blueprint.getOriginalFilename(),
                    storage.load(blueprint.getFilePath()), project.getFloors()));
        } catch (AiServiceException ex) {
            blueprint.markFailed();
            throw ex;
        }
        measurementRepository.deleteAll(
                measurementRepository.findByBlueprintIdAndSource(blueprintId, MeasurementSource.OCR));
        List<BlueprintMeasurement> saved = analysis.measurements().stream()
                .filter(measurement -> measurement.floorNumber() <= project.getFloors())
                .map(measurement -> measurement.toEntity(project, blueprint))
                .toList();
        measurementRepository.saveAllAndFlush(saved);
        // Reads created_at, which the database fills in.
        saved.forEach(entityManager::refresh);
        blueprint.markCompleted();
        int discarded = analysis.discarded() + analysis.measurements().size() - saved.size();
        return BlueprintAnalysisResponse.from(blueprint, analysis, discarded,
                saved.stream().map(MeasurementResponse::from).toList());
    }
}
