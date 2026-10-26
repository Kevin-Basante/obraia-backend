package com.obraia.backend.entity.blueprint;

import java.time.Instant;
import java.util.UUID;

import com.obraia.backend.entity.project.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A blueprint uploaded to a project (photo or PDF). The row keeps only
 * the metadata; the bytes live in the storage named by storageProvider,
 * under the key in filePath.
 */
@Entity
@Table(name = "blueprints")
public class Blueprint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(name = "uploaded_by", updatable = false)
    private UUID uploadedBy;

    @Column(name = "file_path", nullable = false, length = 500, updatable = false)
    private String filePath;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "file_type", nullable = false, length = 15)
    private BlueprintFileType fileType;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_provider", nullable = false, length = 10)
    private StorageProvider storageProvider;

    @Column(name = "processing_status", nullable = false, length = 15)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "uploaded_at", insertable = false, updatable = false)
    private Instant uploadedAt;

    protected Blueprint() {
        // Required by JPA
    }

    public Blueprint(Project project, UUID uploadedBy, String filePath, String originalFilename,
            BlueprintFileType fileType, String mimeType, long sizeBytes, StorageProvider storageProvider) {
        this.project = project;
        this.uploadedBy = uploadedBy;
        this.filePath = filePath;
        this.originalFilename = originalFilename;
        this.fileType = fileType;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.storageProvider = storageProvider;
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public BlueprintFileType getFileType() {
        return fileType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
