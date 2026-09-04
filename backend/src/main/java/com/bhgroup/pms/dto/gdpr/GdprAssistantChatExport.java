package com.bhgroup.pms.dto.gdpr;

import com.bhgroup.pms.domain.AssistantChatStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GdprAssistantChatExport(
        UUID chatId,
        AssistantChatStatus status,
        Instant createdAt,
        Instant lastMessageAt,
        List<GdprAssistantChatMessageExport> messages
) {
}
