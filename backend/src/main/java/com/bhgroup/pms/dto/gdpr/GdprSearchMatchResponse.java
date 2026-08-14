package com.bhgroup.pms.dto.gdpr;

import com.bhgroup.pms.domain.GdprRecordType;
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
