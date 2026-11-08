package com.obraia.backend.entity.notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;

import com.obraia.backend.entity.project.Project;
import com.obraia.backend.util.TextParams;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A message for one user. The project is null for messages about the account. */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", updatable = false)
    private Project project;

    @Column(nullable = false, length = 25, updatable = false)
    private NotificationType type;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String message;

    /** Values of the message as JSON text; the column is jsonb, so the text is cast when it is written. */
    @Column(updatable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String params;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
        // Required by JPA
    }

    public Notification(UUID userId, Project project, NotificationType type, String message, Instant createdAt) {
        this(userId, project, type, message, Map.of(), createdAt);
    }

    public Notification(UUID userId, Project project, NotificationType type, String message,
            Map<String, String> params, Instant createdAt) {
        this.userId = userId;
        this.project = project;
        this.type = type;
        this.message = message;
        this.params = TextParams.toJson(params);
        this.createdAt = createdAt;
    }

    public void markRead() {
        this.read = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Project getProject() {
        return project;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    /** The values of the message; null for the notifications made before they existed. */
    public Map<String, String> getParams() {
        return TextParams.fromJson(params);
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
