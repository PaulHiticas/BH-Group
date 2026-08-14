package com.bhgroup.pms.dto.gdpr;

import java.time.Instant;
import java.util.UUID;

public record GdprLeadExport(
        UUID id,
        String fullName,
        String email,
        String phone,
        String city,
        String message,
        boolean contacted,
        Instant createdAt
) {
}
