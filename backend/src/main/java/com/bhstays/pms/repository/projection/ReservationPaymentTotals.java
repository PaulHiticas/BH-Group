package com.bhstays.pms.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Captured payments of one reservation in one payment currency, together
 * with the reservation's price breakdown snapshot needed to split the
 * accommodation part from fees. {@code accommodationAmount} is null when
 * the reservation has no breakdown.
 */
public record ReservationPaymentTotals(
        UUID propertyId,
        UUID reservationId,
        String paymentCurrency,
        String reservationCurrency,
        BigDecimal reservationTotal,
        BigDecimal accommodationAmount,
        BigDecimal capturedAmount,
        BigDecimal refundedAmount
) {
}
