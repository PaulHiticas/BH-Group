package com.bhstays.pms.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** {@code currencies} has one entry per currency collected in the period - never a blended total. */
public record PropertyCommissionReportResponse(
        UUID propertyId,
        String propertyName,
        LocalDate from,
        LocalDate to,
        BigDecimal commissionPercent,
        boolean commissionConfigured,
        List<PropertyCommissionCurrencyResponse> currencies
) {
}
