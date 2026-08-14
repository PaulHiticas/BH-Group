package com.bhgroup.pms.dto.gdpr;

import com.bhgroup.pms.domain.MessageSenderType;
import java.time.Instant;

public record GdprMessageExport(
        MessageSenderType senderType,
        String body,
        Instant createdAt
) {
}
