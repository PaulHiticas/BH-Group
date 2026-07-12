package com.bhgroup.pms.dto.reservation;

import java.math.BigDecimal;

public record CancellationQuoteResponse(
        BigDecimal refundPercent,
        BigDecimal estimatedRefundAmount,
        String currency
) {
}
