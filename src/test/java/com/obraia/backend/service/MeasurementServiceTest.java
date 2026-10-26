package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.dto.blueprint.MeasurementRequest;
import com.obraia.backend.dto.blueprint.MeasurementResponse;
import com.obraia.backend.entity.blueprint.Blueprint;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.BadRequestException;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.FieldValidationException;
import com.obraia.backend.repository.BlueprintMeasurementRepository;
import com.obraia.backend.repository.BlueprintRepository;
import com.obraia.backend.support.BlueprintFixtures;
import com.obraia.backend.support.ProjectFixtures;

import jakarta.persistence.EntityManager;

class MeasurementServiceTest {

    private final UUID userId = UUID.randomUUID();
    // One floor (see ProjectFixtures).
    private final Project project = ProjectFixtures.project(userId, "Casa Prueba");

    private BlueprintMeasurementRepository measurements;
    private BlueprintRepository blueprints;
    private MeasurementService service;

    @BeforeEach
    void setUp() {
        measurements = mock(BlueprintMeasurementRepository.class);
        blueprints = mock(BlueprintRepository.class);
        ProjectAccessService access = mock(ProjectAccessService.class);
        when(access.requireProject(project.getId(), userId)).thenReturn(project);
        when(measurements.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        service = new MeasurementService(measurements, blueprints, access, mock(EntityManager.class));
    }

    @Test
    void manualWallIsSavedWithItsArea() {
        MeasurementResponse response = service.create(project.getId(), userId,
                request(null, "wall", 1, null, "2.40", 4, "  Muros de fachada  "));

        assertEquals("manual", response.source());
        assertEquals("m2", response.baseUnit());
        assertEquals(new BigDecimal("33.60"), response.amount());
        assertEquals("Muros de fachada", response.notes());
    }

    @Test
    void missingDimensionsAreReportedByField() {
        FieldValidationException ex = assertThrows(FieldValidationException.class,
                () -> service.create(project.getId(), userId, request(null, "slab", 1, null, null, 1, null)));

        assertEquals(Map.of("widthM", "is required for slabs", "heightM", "is required for slabs"), ex.getFields());
        verify(measurements, never()).saveAndFlush(any());
    }

    @Test
    void floorTwoIsRejectedInAOneFloorProject() {
        FieldValidationException ex = assertThrows(FieldValidationException.class,
                () -> service.create(project.getId(), userId, request(null, "wall", 2, null, "2.40", 1, null)));

        assertEquals("the project has only 1 floor", ex.getFields().get("floorNumber"));
    }

    @Test
    void blueprintMustBelongToTheProject() {
        UUID otherBlueprint = UUID.randomUUID();

        FieldValidationException ex = assertThrows(FieldValidationException.class, () -> service.create(
                project.getId(), userId, request(otherBlueprint, "wall", 1, null, "2.40", 1, null)));

        assertEquals("the blueprint does not belong to this project", ex.getFields().get("blueprintId"));
    }

    @Test
    void measurementCanPointToABlueprintOfTheProject() {
        Blueprint blueprint = BlueprintFixtures.blueprint(project, "plano.png");
        when(blueprints.findByIdAndProjectId(blueprint.getId(), project.getId())).thenReturn(Optional.of(blueprint));

        MeasurementResponse response = service.create(project.getId(), userId,
                request(blueprint.getId(), "floor", 1, "10.00", null, 1, null));

        assertEquals("plano.png", response.blueprintFilename());
    }

    @Test
    void unknownElementIsRejected() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.create(project.getId(), userId, request(null, "door", 1, null, "2.00", 1, null)));

        assertEquals("INVALID_ELEMENT_TYPE", ex.getCode());
    }

    @Test
    void aiMeasurementCannotBeEdited() {
        BlueprintMeasurement ai = BlueprintFixtures.measurement(project, MeasurementSource.OCR, ElementType.WALL, 1,
                "4.00", null, "2.40", 1);
        when(measurements.findByIdAndProjectId(ai.getId(), project.getId())).thenReturn(Optional.of(ai));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.update(project.getId(), userId,
                ai.getId(), request(null, "wall", 1, null, "2.50", 1, null)));

        assertEquals("AI_MEASUREMENT_READ_ONLY", ex.getCode());
    }

    @Test
    void editRemovesTheBlueprintLinkWhenItIsNotSent() {
        BlueprintMeasurement manual = BlueprintFixtures.measurement(project, MeasurementSource.MANUAL,
                ElementType.WALL, 1, "4.00", null, "2.40", 1);
        when(measurements.findByIdAndProjectId(manual.getId(), project.getId())).thenReturn(Optional.of(manual));

        MeasurementResponse response = service.update(project.getId(), userId, manual.getId(),
                request(null, "wall", 1, null, "3.00", 2, null));

        assertEquals(new BigDecimal("21.00"), response.amount());
        assertNull(response.blueprintId());
    }

    @Test
    void listFiltersByFloorAndSortsByConstructionOrder() {
        when(measurements.findByProjectIdOrderByCreatedAtAsc(project.getId())).thenReturn(List.of(
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.ROOF, 1, "8.00",
                        "10.00", null, 1),
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.COLUMN, 1, "0.25",
                        "0.25", "2.45", 6),
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.WALL, 2, "4.00", null,
                        "2.40", 1)));

        List<MeasurementResponse> list = service.list(project.getId(), userId, 1, null);

        assertEquals(List.of("column", "roof"), list.stream().map(MeasurementResponse::elementType).toList());
    }

    private static MeasurementRequest request(UUID blueprintId, String element, int floor, String width,
            String height, int quantity, String notes) {
        return new MeasurementRequest(blueprintId, element, floor, new BigDecimal("3.50"),
                width == null ? null : new BigDecimal(width), height == null ? null : new BigDecimal(height),
                quantity, notes);
    }
}
