package com.bhgroup.pms.dto.lead;

import com.bhgroup.pms.domain.LeadType;
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
