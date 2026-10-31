package com.obraia.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.notification.NotificationResponse;
import com.obraia.backend.dto.notification.UnreadCountResponse;
import com.obraia.backend.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** The notifications of the signed-in user. */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Automatic messages about budget, risk and approvals")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Latest 50 notifications, newest first")
    @ApiResponse(responseCode = "200", description = "Notifications; empty when there are none")
    public List<NotificationResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.list(ProjectController.userId(jwt));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "How many notifications are not read yet")
    @ApiResponse(responseCode = "200", description = "Unread count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.unreadCount(ProjectController.userId(jwt));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark one notification as read")
    @ApiResponse(responseCode = "200", description = "Notification marked as read")
    @ApiResponse(responseCode = "404", description = "The notification does not exist or is of another user",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public NotificationResponse markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID notificationId) {
        return notificationService.markRead(ProjectController.userId(jwt), notificationId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark every notification as read")
    @ApiResponse(responseCode = "204", description = "All notifications marked as read")
    public void markAllRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllRead(ProjectController.userId(jwt));
    }
}
