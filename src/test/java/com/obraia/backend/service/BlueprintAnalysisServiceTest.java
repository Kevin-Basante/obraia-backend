package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.util.unit.DataSize;

import com.obraia.backend.ai.AiMeasurement;
import com.obraia.backend.ai.AiServiceClient;
import com.obraia.backend.ai.AnalysisRequest;
import com.obraia.backend.ai.BlueprintAnalysis;
import com.obraia.backend.config.StorageProperties;
import com.obraia.backend.dto.blueprint.BlueprintAnalysisResponse;
import com.obraia.backend.entity.blueprint.Blueprint;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.blueprint.ProcessingStatus;
import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectStatus;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.BlueprintMeasurementRepository;
import com.obraia.backend.repository.BlueprintRepository;
import com.obraia.backend.storage.BlueprintStorage;
import com.obraia.backend.support.BlueprintFixtures;
import com.obraia.backend.support.ProjectFixtures;

import jakarta.persistence.EntityManager;

class BlueprintAnalysisServiceTest {

    private static final AiMeasurement WALL = new AiMeasurement(ElementType.WALL, 1, new BigDecimal("4.40"), null,
            new BigDecimal("2.50"), 1, "Muro de fachada");

    private final UUID userId = UUID.randomUUID();

    private Project project;
    private Blueprint blueprint;
    private BlueprintRepository blueprints;
    private BlueprintMeasurementRepository measurements;
    private BlueprintStorage storage;
    private AiServiceClient aiService;
    private List<BlueprintMeasurement> saved;
    private BlueprintAnalysisService service;

    @BeforeEach
    void setUp() {
        setUpProject(ProjectFixtures.project(userId, "Casa IA"));
    }

    private void setUpProject(Project withStatus) {
        project = withStatus;
        blueprint = BlueprintFixtures.blueprint(project, "planta.png");
        blueprints = mock(BlueprintRepository.class);
        measurements = mock(BlueprintMeasurementRepository.class);
        storage = mock(BlueprintStorage.class);
        aiService = mock(AiServiceClient.class);
        saved = new ArrayList<>();
        when(blueprints.findByIdAndProjectId(blueprint.getId(), project.getId())).thenReturn(Optional.of(blueprint));
        when(storage.load(blueprint.getFilePath())).thenReturn(BlueprintFixtures.PNG);
        when(measurements.saveAllAndFlush(any())).thenAnswer(call -> {
            saved.addAll(call.getArgument(0));
            return call.getArgument(0);
        });
        BlueprintService blueprintService = new BlueprintService(blueprints, mock(ProjectAccessService.class),
                storage, new StorageProperties("database", "uploads", DataSize.ofMegabytes(10), 10),
                mock(EntityManager.class));
        service = new BlueprintAnalysisService(blueprintService, measurements, storage, aiService,
                mock(EntityManager.class));
    }

    @Test
    void savesWhatTheAiReadAsOcrMeasurementsOfTheBlueprint() {
        when(aiService.analyze(any())).thenReturn(analysis(List.of(WALL), false));

        BlueprintAnalysisResponse response = service.analyze(project.getId(), userId, blueprint.getId());

        assertEquals(1, saved.size());
        BlueprintMeasurement measurement = saved.get(0);
        assertEquals(MeasurementSource.OCR, measurement.getSource());
        assertEquals(blueprint, measurement.getBlueprint());
        assertEquals(new BigDecimal("4.40"), measurement.getLengthM());
        assertEquals(ProcessingStatus.COMPLETED, blueprint.getProcessingStatus());
        assertEquals("completed", response.processingStatus().code());
        assertEquals(1, response.measurements().size());
        assertEquals("ocr", response.measurements().get(0).source());
        assertEquals(List.of("Escala no visible"), response.warnings());
        assertEquals("gemini-test", response.model());
        assertFalse(response.cached());
    }

    @Test
    void sendsTheFileAndTheFloorsOfTheProject() {
        when(aiService.analyze(any())).thenReturn(analysis(List.of(), false));

        service.analyze(project.getId(), userId, blueprint.getId());

        ArgumentCaptor<AnalysisRequest> request = ArgumentCaptor.forClass(AnalysisRequest.class);
        verify(aiService).analyze(request.capture());
        assertEquals(project.getId(), request.getValue().projectId());
        assertEquals("planta.png", request.getValue().filename());
        assertEquals(BlueprintFixtures.PNG, request.getValue().content());
        assertEquals(1, request.getValue().floors());
    }

    @Test
    void analyzingAgainReplacesWhatTheAiReadBefore() {
        List<BlueprintMeasurement> previous = List.of(
                BlueprintFixtures.measurement(project, MeasurementSource.OCR, ElementType.WALL, 1, "3", null, "2.4",
                        1));
        when(measurements.findByBlueprintIdAndSource(blueprint.getId(), MeasurementSource.OCR)).thenReturn(previous);
        when(aiService.analyze(any())).thenReturn(analysis(List.of(WALL), true));

        BlueprintAnalysisResponse response = service.analyze(project.getId(), userId, blueprint.getId());

        verify(measurements).deleteAll(previous);
        assertEquals(1, saved.size());
        assertEquals(true, response.cached());
    }

    @Test
    void elementsOnAFloorTheProjectDoesNotHaveAreDiscarded() {
        AiMeasurement upstairs = new AiMeasurement(ElementType.FLOOR, 2, new BigDecimal("8.00"),
                new BigDecimal("6.00"), null, 1, null);
        when(aiService.analyze(any())).thenReturn(analysis(List.of(WALL, upstairs), false));

        BlueprintAnalysisResponse response = service.analyze(project.getId(), userId, blueprint.getId());

        assertEquals(1, saved.size());
        // One discarded by obraia-ia and one here.
        assertEquals(2, response.discarded());
    }

    @Test
    void blueprintOfAnotherProjectIsNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.analyze(project.getId(), userId, UUID.randomUUID()));

        verify(aiService, never()).analyze(any());
    }

    @Test
    void blueprintsOfAClosedProjectCannotBeAnalyzed() {
        setUpProject(ProjectFixtures.project(userId, "Casa Terminada", ProjectStatus.COMPLETED));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.analyze(project.getId(), userId, blueprint.getId()));

        assertEquals("PROJECT_CLOSED", ex.getCode());
        verify(aiService, never()).analyze(any());
    }

    private static BlueprintAnalysis analysis(List<AiMeasurement> measurements, boolean cached) {
        return new BlueprintAnalysis(measurements, List.of("Escala no visible"), 1, "gemini-test", cached);
    }
}
