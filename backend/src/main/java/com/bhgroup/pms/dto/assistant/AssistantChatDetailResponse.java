package com.bhgroup.pms.dto.assistant;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssistantChatDetailResponse(
        UUID id,
        String guestName,
        String guestEmail,
        String status,
        Instant lastMessageAt,
        Instant createdAt,
        List<AssistantChatMessageResponse> messages
) {
}
