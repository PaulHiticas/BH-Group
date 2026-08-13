package com.bhgroup.pms.dto.latecheckout;

import com.bhgroup.pms.domain.LateCheckoutStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record LateCheckoutRequestResponse(
        UUID id,
        UUID reservationId,
        LocalTime requestedCheckoutTime,
        BigDecimal fee,
        String currency,
        LateCheckoutStatus status,
        String guestNote,
        Instant createdAt,
        Instant decidedAt,
        Instant paidAt
) {
}
