package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.obraia.backend.dto.notification.NotificationResponse;
import com.obraia.backend.entity.User;
import com.obraia.backend.entity.notification.Notification;
import com.obraia.backend.entity.notification.NotificationType;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.NotificationRepository;
import com.obraia.backend.repository.ProjectMemberRepository;
import com.obraia.backend.support.ProjectFixtures;
import com.obraia.backend.support.TestUsers;

class NotificationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"),
            ZoneId.of("America/Bogota"));

    private final UUID userId = UUID.randomUUID();
    private final Project project = ProjectFixtures.project(userId, "Casa Avisos");

    private NotificationRepository notifications;
    private ProjectMemberRepository members;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        notifications = mock(NotificationRepository.class);
        members = mock(ProjectMemberRepository.class);
        service = new NotificationService(notifications, members, CLOCK);
    }

    @Test
    void everyMemberGetsTheMessage() {
        User owner = TestUsers.withId(TestUsers.verifiedUser("hash"));
        User engineer = TestUsers.withId(TestUsers.verifiedUser("hash"));
        when(members.findByProjectIdOrderByJoinedAtAsc(project.getId())).thenReturn(List.of(
                ProjectFixtures.member(project, owner), ProjectFixtures.member(project, engineer)));

        int sent = service.notifyMembers(project.getId(), NotificationType.DELAY_RISK, "The risk rose to high");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> saved = ArgumentCaptor.forClass(List.class);
        verify(notifications).saveAll(saved.capture());
        assertEquals(2, sent);
        assertEquals(List.of(owner.getId(), engineer.getId()),
                saved.getValue().stream().map(Notification::getUserId).toList());
        Notification first = saved.getValue().get(0);
        assertEquals(NotificationType.DELAY_RISK, first.getType());
        assertEquals(project, first.getProject());
        assertEquals(CLOCK.instant(), first.getCreatedAt());
        assertEquals(false, first.isRead());
    }

    @Test
    void aProjectWithoutMembersSendsNothing() {
        when(members.findByProjectIdOrderByJoinedAtAsc(project.getId())).thenReturn(List.of());

        assertEquals(0, service.notifyMembers(project.getId(), NotificationType.SYSTEM, "Hello"));
        verify(notifications).saveAll(anyList());
    }

    @Test
    void listShowsTheProjectOfEachNotification() {
        when(notifications.findTop50ByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(
                new Notification(userId, project, NotificationType.BUDGET_APPROVED, "Approved", CLOCK.instant()),
                new Notification(userId, null, NotificationType.SYSTEM, "Welcome", CLOCK.instant())));

        List<NotificationResponse> list = service.list(userId);

        assertEquals("budget_approved", list.get(0).type().code());
        assertEquals("Casa Avisos", list.get(0).projectName());
        assertNull(list.get(1).projectId());
    }

    @Test
    void markReadChangesOnlyTheNotificationOfTheUser() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(userId, project, NotificationType.SYSTEM, "Hi",
                CLOCK.instant());
        when(notifications.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.of(notification));

        assertTrue(service.markRead(userId, notificationId).read());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.markRead(UUID.randomUUID(), notificationId));
        assertEquals("NOTIFICATION_NOT_FOUND", ex.getCode());
    }

    @Test
    void unreadCountAndMarkAllRead() {
        when(notifications.countByUserIdAndReadFalse(userId)).thenReturn(3L);

        assertEquals(3, service.unreadCount(userId).count());
        service.markAllRead(userId);

        verify(notifications).markAllRead(userId);
    }
}
