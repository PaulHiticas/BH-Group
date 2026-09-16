package com.bhstays.pms.dto.payment;

import com.bhstays.pms.domain.RefundStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        BigDecimal amount,
        String reason,
        RefundStatus status,
        Instant createdAt
) {
}
