package com.bhstays.pms.dto.notification;

import com.bhstays.pms.domain.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String body,
        String linkPath,
        Instant readAt,
        Instant createdAt
) {
}
