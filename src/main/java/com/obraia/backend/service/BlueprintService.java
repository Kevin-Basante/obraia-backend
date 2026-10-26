package com.obraia.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.config.StorageProperties;
import com.obraia.backend.dto.blueprint.BlueprintResponse;
import com.obraia.backend.entity.blueprint.Blueprint;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.BadRequestException;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.FileTooLargeException;
import com.obraia.backend.repository.BlueprintRepository;
import com.obraia.backend.storage.BlueprintStorage;
import com.obraia.backend.storage.FileTypeDetector;
import com.obraia.backend.storage.FileTypeDetector.DetectedFile;

import jakarta.persistence.EntityManager;

/**
 * Blueprints of a project: photos or PDFs that any member can upload,
 * open and delete. The bytes go to BlueprintStorage and the metadata to
 * the blueprints table, in the same transaction.
 */
@Service
public class BlueprintService {

    private final BlueprintRepository blueprintRepository;
    private final ProjectAccessService accessService;
    private final BlueprintStorage storage;
    private final StorageProperties properties;
    private final EntityManager entityManager;

    public BlueprintService(BlueprintRepository blueprintRepository, ProjectAccessService accessService,
            BlueprintStorage storage, StorageProperties properties, EntityManager entityManager) {
        this.blueprintRepository = blueprintRepository;
        this.accessService = accessService;
        this.storage = storage;
        this.properties = properties;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<BlueprintResponse> list(UUID projectId, UUID userId) {
        accessService.requireMembership(projectId, userId);
        return blueprintRepository.findByProjectIdOrderByUploadedAtDesc(projectId).stream()
                .map(BlueprintResponse::from)
                .toList();
    }

    /** The real type comes from the first bytes, never from the name or the browser. */
    @Transactional
    public BlueprintResponse upload(UUID projectId, UUID userId, String filename, byte[] content) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        if (content.length == 0) {
            throw new BadRequestException("EMPTY_FILE", "The file is empty");
        }
        if (content.length > properties.maxFileSize().toBytes()) {
            throw new FileTooLargeException(properties.maxFileSize());
        }
        if (blueprintRepository.countByProjectId(projectId) >= properties.maxFilesPerProject()) {
            throw new ConflictException("BLUEPRINT_LIMIT_REACHED",
                    "A project can have up to " + properties.maxFilesPerProject() + " blueprints");
        }
        DetectedFile detected = FileTypeDetector.detect(content)
                .orElseThrow(() -> new BadRequestException("UNSUPPORTED_FILE_TYPE",
                        "Upload a PDF or an image (PNG, JPG or WEBP)"));
        String key = "projects/" + projectId + "/" + UUID.randomUUID() + "." + detected.extension();
        Blueprint blueprint = blueprintRepository.saveAndFlush(new Blueprint(project, userId, key,
                cleanFilename(filename, detected), detected.fileType(), detected.mimeType(), content.length,
                storage.provider()));
        storage.save(key, content);
        // Reads uploaded_at, which the database fills in.
        entityManager.refresh(blueprint);
        return BlueprintResponse.from(blueprint);
    }

    /**
     * Keeps only the name of the file, without the folders some browsers
     * send, and makes sure it has the extension of its real type.
     */
    static String cleanFilename(String filename, DetectedFile detected) {
        String name = filename == null ? "" : filename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).strip();
        if (name.isEmpty()) {
            name = "blueprint";
        }
        if (!name.toLowerCase().endsWith("." + detected.extension())
                && !(detected.extension().equals("jpg") && name.toLowerCase().endsWith(".jpeg"))) {
            name = name + "." + detected.extension();
        }
        if (name.length() > 255) {
            name = name.substring(name.length() - 255);
        }
        return name;
    }
}
