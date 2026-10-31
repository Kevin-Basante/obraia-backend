package com.obraia.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.obraia.backend.dto.notification.NotificationResponse;
import com.obraia.backend.dto.notification.UnreadCountResponse;
import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.service.NotificationService;

/** HTTP side of the notification routes, without the database. */
class NotificationControllerTest {

    private final UUID userId = UUID.randomUUID();

    private NotificationService notificationService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        notificationService = mock(NotificationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static NotificationResponse notification(boolean read) {
        return new NotificationResponse(UUID.randomUUID(), new StatusResponse("delay_risk", "Risk of delay"),
                "The risk of \"Casa Avisos\" rose to high", UUID.randomUUID(), "Casa Avisos", read,
                Instant.parse("2026-11-20T15:00:00Z"));
    }

    @Test
    void listReturnsTheNotificationsOfTheUser() throws Exception {
        when(notificationService.list(userId)).thenReturn(List.of(notification(false)));

        mvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type.code").value("delay_risk"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void unreadCount() throws Exception {
        when(notificationService.unreadCount(userId)).thenReturn(new UnreadCountResponse(4));

        mvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    void markReadAndUnknownNotification() throws Exception {
        UUID notificationId = UUID.randomUUID();
        when(notificationService.markRead(any(), any(UUID.class)))
                .thenReturn(notification(true))
                .thenThrow(new ResourceNotFoundException("NOTIFICATION_NOT_FOUND", "Not found"));

        mvc.perform(patch("/api/notifications/{id}/read", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
        mvc.perform(patch("/api/notifications/{id}/read", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    void readAllAnswers204() throws Exception {
        mvc.perform(post("/api/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllRead(userId);
    }
}
