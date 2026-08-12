package com.bhgroup.pms.dto.ownerstatement;

import java.math.BigDecimal;
import java.util.UUID;

public record OwnerStatementLineResponse(
        UUID propertyId,
        String propertyName,
        BigDecimal grossRevenue,
        BigDecimal commissionAmount,
        BigDecimal expensesTotal,
        BigDecimal netAmount
) {
}
