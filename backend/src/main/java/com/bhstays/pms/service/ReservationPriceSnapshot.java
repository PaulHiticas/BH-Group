package com.bhstays.pms.service;

import com.bhstays.pms.domain.Reservation;
import com.bhstays.pms.dto.property.PriceQuoteResponse;
import java.math.BigDecimal;

/**
 * Keeps a reservation's price breakdown (accommodation / cleaning fee /
 * extra-guest fee) in step with its {@code totalAmount}. The breakdown is
 * only ever copied from a server-side quote whose total is exactly the
 * reservation's total in the same currency; in every other case it is
 * cleared, so the commission report treats the split as unknown instead of
 * trusting a stale or guessed one.
 */
final class ReservationPriceSnapshot {

    private ReservationPriceSnapshot() {
    }

    static void apply(Reservation reservation, PriceQuoteResponse quote) {
        if (!matches(reservation, quote)) {
            clear(reservation);
            return;
        }
        BigDecimal cleaningFee = orZero(quote.cleaningFee());
        BigDecimal extraGuestFee = orZero(quote.extraGuestFee());
        BigDecimal accommodation = reservation.getTotalAmount().subtract(cleaningFee).subtract(extraGuestFee);
        if (accommodation.signum() < 0) {
            clear(reservation);
            return;
        }
        reservation.setAccommodationAmount(accommodation);
        reservation.setCleaningFeeAmount(cleaningFee);
        reservation.setExtraGuestFeeAmount(extraGuestFee);
    }

    private static boolean matches(Reservation reservation, PriceQuoteResponse quote) {
        return quote != null
                && quote.available()
                && quote.totalAmount() != null
                && reservation.getTotalAmount() != null
                && quote.totalAmount().compareTo(reservation.getTotalAmount()) == 0
                && quote.currency() != null
                && quote.currency().equalsIgnoreCase(reservation.getCurrency());
    }

    private static void clear(Reservation reservation) {
        reservation.setAccommodationAmount(null);
        reservation.setCleaningFeeAmount(null);
        reservation.setExtraGuestFeeAmount(null);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
