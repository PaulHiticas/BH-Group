package com.bhgroup.pms.dto.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DynamicPriceBreakdownResponse(
        boolean available,
        String unavailableReason,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int nights,
        List<NightlyPriceBreakdown> nightlyBreakdown,
        BigDecimal subtotal,
        BigDecimal extraGuestFee,
        BigDecimal cleaningFee,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String currency
) {
}
