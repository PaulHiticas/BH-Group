package com.bhgroup.pms.dto.ownerstatement;

import com.bhgroup.pms.domain.OwnerStatementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OwnerStatementSummaryResponse(
        UUID id,
        UUID ownerId,
        String ownerName,
        LocalDate periodStart,
        LocalDate periodEnd,
        String currency,
        BigDecimal netPayout,
        OwnerStatementStatus status,
        Instant createdAt,
        Instant paidAt
) {
}
