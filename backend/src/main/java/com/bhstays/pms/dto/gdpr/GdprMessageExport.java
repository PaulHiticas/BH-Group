package com.bhstays.pms.dto.gdpr;

import com.bhstays.pms.domain.MessageSenderType;
import java.time.Instant;

public record GdprMessageExport(
        MessageSenderType senderType,
        String body,
        Instant createdAt
) {
}
