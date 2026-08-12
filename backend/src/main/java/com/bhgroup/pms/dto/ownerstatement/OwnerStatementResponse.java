package com.bhgroup.pms.dto.ownerstatement;

import com.bhgroup.pms.domain.OwnerStatementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OwnerStatementResponse(
        UUID id,
        UUID ownerId,
        String ownerName,
        LocalDate periodStart,
        LocalDate periodEnd,
        String currency,
        BigDecimal grossRevenue,
        BigDecimal commissionAmount,
        BigDecimal expensesTotal,
        BigDecimal netPayout,
        OwnerStatementStatus status,
        String generatedByName,
        Instant paidAt,
        String paymentReference,
        Instant createdAt,
        List<OwnerStatementLineResponse> lines
) {
}
