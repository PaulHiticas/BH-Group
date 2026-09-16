package com.bhstays.pms.dto.lead;

import com.bhstays.pms.domain.LeadType;
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
        LeadType leadType,
        Integer bedrooms,
        boolean consentGiven,
        String utmSource,
        String utmMedium,
        String utmCampaign,
        Instant createdAt
) {
}
