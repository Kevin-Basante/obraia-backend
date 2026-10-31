package com.obraia.backend.dto.notification;

import java.time.Instant;
import java.util.UUID;

import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.notification.Notification;
import com.obraia.backend.entity.project.Project;

/** A notification; the project fields are null for messages about the account. */
public record NotificationResponse(UUID id, StatusResponse type, String message, UUID projectId,
        String projectName, boolean read, Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        Project project = notification.getProject();
        return new NotificationResponse(notification.getId(),
                new StatusResponse(notification.getType().getCode(), notification.getType().getLabel()),
                notification.getMessage(), project == null ? null : project.getId(),
                project == null ? null : project.getName(), notification.isRead(), notification.getCreatedAt());
    }
}
