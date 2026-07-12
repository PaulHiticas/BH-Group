package com.bhgroup.pms.service;

import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.Notification;
import com.bhgroup.pms.domain.NotificationType;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.domain.UserStatus;
import com.bhgroup.pms.dto.notification.NotificationResponse;
import com.bhgroup.pms.repository.NotificationRepository;
import com.bhgroup.pms.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final List<Role> ADMIN_ROLES = List.of(Role.SUPER_ADMIN, Role.ADMINISTRATOR);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(userId))
                .ifPresent(n -> {
                    n.setReadAt(Instant.now());
                    notificationRepository.save(n);
                });
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadForUser(userId, Instant.now());
    }

    /** Notifies every SUPER_ADMIN/ADMINISTRATOR account. */
    @Transactional
    public void notifyAdmins(NotificationType type, String title, String body, String linkPath) {
        notifyRoles(ADMIN_ROLES, type, title, body, linkPath);
    }

    /** Notifies every active account holding one of the given roles. */
    @Transactional
    public void notifyRoles(List<Role> roles, NotificationType type, String title, String body, String linkPath) {
        for (User user : userRepository.findByRoleInAndStatus(roles, UserStatus.ACTIVE)) {
            notificationRepository.save(Notification.builder()
                    .user(user)
                    .type(type)
                    .title(title)
                    .body(body)
                    .linkPath(linkPath)
                    .build());
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(), notification.getType(), notification.getTitle(), notification.getBody(),
                notification.getLinkPath(), notification.getReadAt(), notification.getCreatedAt());
    }
}
