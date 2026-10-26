package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import com.obraia.backend.config.StorageProperties;
import com.obraia.backend.dto.blueprint.BlueprintResponse;
import com.obraia.backend.entity.blueprint.Blueprint;
import com.obraia.backend.entity.blueprint.BlueprintFileType;
import com.obraia.backend.entity.blueprint.StorageProvider;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectStatus;
import com.obraia.backend.exception.AppException;
import com.obraia.backend.exception.BadRequestException;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.BlueprintRepository;
import com.obraia.backend.storage.BlueprintStorage;
import com.obraia.backend.storage.FileTypeDetector.DetectedFile;
import com.obraia.backend.support.BlueprintFixtures;
import com.obraia.backend.support.ProjectFixtures;

import jakarta.persistence.EntityManager;

class BlueprintServiceTest {

    private static final DetectedFile PDF = new DetectedFile(BlueprintFileType.VECTOR_PDF, "application/pdf", "pdf");

    private final UUID userId = UUID.randomUUID();
    private final Project project = ProjectFixtures.project(userId, "Casa Prueba");

    private BlueprintRepository blueprints;
    private BlueprintStorage storage;
    private ProjectAccessService access;
    private BlueprintService service;

    @BeforeEach
    void setUp() {
        blueprints = mock(BlueprintRepository.class);
        storage = mock(BlueprintStorage.class);
        access = mock(ProjectAccessService.class);
        when(storage.provider()).thenReturn(StorageProvider.EXTERNAL);
        when(access.requireProject(project.getId(), userId)).thenReturn(project);
        when(blueprints.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        StorageProperties properties = new StorageProperties("database", "uploads", DataSize.ofBytes(20), 2);
        service = new BlueprintService(blueprints, access, storage, properties, mock(EntityManager.class));
    }

    @Test
    void uploadSavesTheBytesUnderAKeyOfTheProject() {
        BlueprintResponse response = service.upload(project.getId(), userId, "C:\\planos\\primer piso.png",
                BlueprintFixtures.PNG);

        assertEquals("primer piso.png", response.originalFilename());
        assertEquals("image", response.fileType());
        assertEquals("pending", response.processingStatus().code());
        verify(storage).save(startsWith("projects/" + project.getId() + "/"),
                eq(BlueprintFixtures.PNG));
    }

    @Test
    void fileThatIsNotAnImageOrPdfIsRejected() {
        byte[] text = "hello".getBytes(StandardCharsets.US_ASCII);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.upload(project.getId(), userId, "plano.pdf", text));

        assertEquals("UNSUPPORTED_FILE_TYPE", ex.getCode());
        verify(storage, never()).save(anyString(), any());
    }

    @Test
    void emptyFileIsRejected() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.upload(project.getId(), userId, "plano.png", new byte[0]));

        assertEquals("EMPTY_FILE", ex.getCode());
    }

    @Test
    void fileOverTheLimitAnswers413() {
        AppException ex = assertThrows(AppException.class,
                () -> service.upload(project.getId(), userId, "plano.png", new byte[21]));

        assertEquals("FILE_TOO_LARGE", ex.getCode());
        assertEquals(413, ex.getStatus().value());
    }

    @Test
    void projectWithTheMaximumOfBlueprintsRejectsMore() {
        when(blueprints.countByProjectId(project.getId())).thenReturn(2L);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.upload(project.getId(), userId, "plano.png", BlueprintFixtures.PNG));

        assertEquals("BLUEPRINT_LIMIT_REACHED", ex.getCode());
    }

    @Test
    void closedProjectDoesNotAcceptBlueprints() {
        Project closed = ProjectFixtures.project(userId, "Casa Cerrada", ProjectStatus.COMPLETED);
        when(access.requireProject(closed.getId(), userId)).thenReturn(closed);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.upload(closed.getId(), userId, "plano.png", BlueprintFixtures.PNG));

        assertEquals("PROJECT_CLOSED", ex.getCode());
    }

    @Test
    void downloadReadsTheStoredBytes() {
        Blueprint blueprint = BlueprintFixtures.blueprint(project, "plano.png");
        when(blueprints.findByIdAndProjectId(blueprint.getId(), project.getId())).thenReturn(Optional.of(blueprint));
        when(storage.load(blueprint.getFilePath())).thenReturn(BlueprintFixtures.PNG);

        BlueprintService.BlueprintFile file = service.download(project.getId(), userId, blueprint.getId());

        assertEquals("image/png", file.mimeType());
        assertEquals(BlueprintFixtures.PNG.length, file.content().length);
    }

    @Test
    void blueprintOfAnotherProjectAnswers404() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.download(project.getId(), userId, UUID.randomUUID()));

        assertEquals("BLUEPRINT_NOT_FOUND", ex.getCode());
    }

    @Test
    void deleteRemovesTheRowAndTheFile() {
        Blueprint blueprint = BlueprintFixtures.blueprint(project, "plano.png");
        when(blueprints.findByIdAndProjectId(blueprint.getId(), project.getId())).thenReturn(Optional.of(blueprint));

        service.delete(project.getId(), userId, blueprint.getId());

        verify(blueprints).delete(blueprint);
        verify(storage).delete(blueprint.getFilePath());
    }

    @Test
    void cleanFilenameAddsTheRealExtension() {
        assertEquals("plano.pdf", BlueprintService.cleanFilename("plano", PDF));
        assertEquals("blueprint.pdf", BlueprintService.cleanFilename("  ", PDF));
        assertEquals("foto.jpeg", BlueprintService.cleanFilename("foto.jpeg",
                new DetectedFile(BlueprintFileType.IMAGE, "image/jpeg", "jpg")));
        assertTrue(BlueprintService.cleanFilename("a".repeat(300) + ".pdf", PDF).length() <= 255);
    }
}
