package com.obraia.backend.dto.notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.notification.Notification;
import com.obraia.backend.entity.project.Project;

/**
 * A notification; the project fields are null for messages about the account.
 * The params hold the values of the message, so the frontend can write it in
 * the language of the user; they are null for older notifications.
 */
public record NotificationResponse(UUID id, StatusResponse type, String message, Map<String, String> params,
        UUID projectId, String projectName, boolean read, Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        Project project = notification.getProject();
        return new NotificationResponse(notification.getId(),
                new StatusResponse(notification.getType().getCode(), notification.getType().getLabel()),
                notification.getMessage(), notification.getParams(), project == null ? null : project.getId(),
                project == null ? null : project.getName(), notification.isRead(), notification.getCreatedAt());
    }
}
