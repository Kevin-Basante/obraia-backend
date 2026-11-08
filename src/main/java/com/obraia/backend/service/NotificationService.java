package com.obraia.backend.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.notification.NotificationResponse;
import com.obraia.backend.dto.notification.UnreadCountResponse;
import com.obraia.backend.entity.notification.Notification;
import com.obraia.backend.entity.notification.NotificationType;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.NotificationRepository;
import com.obraia.backend.repository.ProjectMemberRepository;

/** The notifications of the signed-in user. */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProjectMemberRepository memberRepository;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository,
            ProjectMemberRepository memberRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    /** Sends the same message, with the values of its sentence, to every member of the project. */
    @Transactional
    public int notifyMembers(UUID projectId, NotificationType type, String message, Map<String, String> params) {
        List<Notification> notifications = memberRepository.findByProjectIdOrderByJoinedAtAsc(projectId).stream()
                .filter(member -> member.getUser().getDeletedAt() == null)
                .map(member -> new Notification(member.getUser().getId(), member.getProject(), type, message,
                        params, clock.instant()))
                .toList();
        notificationRepository.saveAll(notifications);
        return notifications.size();
    }

    /** The latest 50, newest first. */
    @Transactional(readOnly = true)
    public List<NotificationResponse> list(UUID userId) {
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        return new UnreadCountResponse(notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("NOTIFICATION_NOT_FOUND",
                        "The notification " + notificationId + " does not exist"));
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId);
    }
}
