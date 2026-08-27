package com.bhgroup.pms.dto.assistant;

import java.time.Instant;
import java.util.UUID;

public record AssistantChatSummaryResponse(
        UUID id,
        String guestName,
        String guestEmail,
        String status,
        Instant lastMessageAt,
        Instant createdAt
) {
}
