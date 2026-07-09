package com.bhgroup.pms.lead.dto;

import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
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
