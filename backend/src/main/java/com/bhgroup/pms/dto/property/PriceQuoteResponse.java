package com.bhgroup.pms.dto.property;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceQuoteResponse(
        boolean available,
        String unavailableReason,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int nights,
        BigDecimal subtotal,
        BigDecimal extraGuestFee,
        BigDecimal cleaningFee,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String currency,
        Integer minStayNights,
        Integer maxStayNights
) {
}
