package com.bhstays.pms.dto.gdpr;

import com.bhstays.pms.domain.GdprRecordType;
import java.time.Instant;
import java.util.UUID;

public record GdprSearchMatchResponse(
        GdprRecordType recordType,
        UUID id,
        String name,
        String email,
        String phone,
        String context,
        Instant createdAt
) {
}
