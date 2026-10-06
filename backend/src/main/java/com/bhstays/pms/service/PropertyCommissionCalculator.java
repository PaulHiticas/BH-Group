package com.bhstays.pms.service;

import com.bhstays.pms.dto.report.PropertyCommissionCurrencyResponse;
import com.bhstays.pms.repository.projection.ReservationPaymentTotals;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Splits one property's collected money into the owner's share and BH
 * Stays's commission, separately per currency (amounts in different
 * currencies are never added together, and nothing is converted).
 *
 * <pre>
 * netRevenue          = captured - successful refunds
 * commissionableBase  = accommodation part of what was captured, after refunds
 * bhStaysRevenue      = commissionableBase * commissionPercent / 100
 * ownerAmount         = netRevenue - bhStaysRevenue
 * </pre>
 *
 * <p>Per reservation, the accommodation part of a capture is the
 * reservation's accommodation share of its total (capped at the
 * accommodation amount if more than the total was paid). Refunds carry no
 * allocation to price components, so a partial refund reduces that base
 * proportionally: {@code base = capturedAccommodation * net / captured}.
 * Cleaning fee, extra-guest fee and any other non-accommodation amount are
 * therefore never commissioned.
 *
 * <p>A reservation without a price breakdown - or paid in a currency other
 * than its own - has no verifiable accommodation share: its net amount is
 * reported as {@code unallocatedNetRevenue} and left out of the base.
 *
 * <p>Rounding: intermediate ratios at scale {@value #INTERMEDIATE_SCALE},
 * each reservation's base and each property/currency commission rounded to
 * 2 decimals HALF_UP; portfolio totals are sums of those rounded values, so
 * they reconcile exactly with the per-property figures.
 */
public final class PropertyCommissionCalculator {

    static final int MONEY_SCALE = 2;
    static final int INTERMEDIATE_SCALE = 10;
    static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private PropertyCommissionCalculator() {
    }

    /** Rows must all belong to the same property; the result is sorted by currency. */
    public static List<PropertyCommissionCurrencyResponse> calculate(BigDecimal commissionPercent,
                                                                     List<ReservationPaymentTotals> rows) {
        Map<String, Accumulator> byCurrency = new TreeMap<>();
        for (ReservationPaymentTotals row : rows) {
            byCurrency.computeIfAbsent(normalizeCurrency(row.paymentCurrency()), currency -> new Accumulator())
                    .add(row);
        }

        List<PropertyCommissionCurrencyResponse> result = new ArrayList<>();
        byCurrency.forEach((currency, acc) -> result.add(acc.toResponse(currency, commissionPercent)));
        return result;
    }

    /**
     * The accommodation part of a reservation's captured money left after
     * refunds, or null when it cannot be determined from the snapshot.
     */
    static BigDecimal commissionableBase(ReservationPaymentTotals row) {
        BigDecimal captured = orZero(row.capturedAmount());
        BigDecimal total = row.reservationTotal();
        BigDecimal accommodation = row.accommodationAmount();
        if (accommodation == null || total == null || total.signum() <= 0 || captured.signum() <= 0
                || !normalizeCurrency(row.paymentCurrency()).equals(normalizeCurrency(row.reservationCurrency()))) {
            return null;
        }

        BigDecimal capturedAccommodation = captured.multiply(accommodation)
                .divide(total, INTERMEDIATE_SCALE, ROUNDING)
                .min(accommodation);
        BigDecimal net = captured.subtract(orZero(row.refundedAmount()));
        return capturedAccommodation.multiply(net)
                .divide(captured, INTERMEDIATE_SCALE, ROUNDING)
                .setScale(MONEY_SCALE, ROUNDING);
    }

    static BigDecimal commission(BigDecimal base, BigDecimal commissionPercent) {
        return base.multiply(commissionPercent).divide(ONE_HUNDRED, MONEY_SCALE, ROUNDING);
    }

    static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING);
    }

    private static String normalizeCurrency(String currency) {
        return currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static final class Accumulator {
        private BigDecimal captured = BigDecimal.ZERO;
        private BigDecimal refunded = BigDecimal.ZERO;
        private BigDecimal base = BigDecimal.ZERO;
        private BigDecimal unallocatedNet = BigDecimal.ZERO;
        private int reservations;

        void add(ReservationPaymentTotals row) {
            BigDecimal rowCaptured = orZero(row.capturedAmount());
            BigDecimal rowRefunded = orZero(row.refundedAmount());
            captured = captured.add(rowCaptured);
            refunded = refunded.add(rowRefunded);
            reservations++;

            BigDecimal rowBase = commissionableBase(row);
            if (rowBase != null) {
                base = base.add(rowBase);
            } else {
                unallocatedNet = unallocatedNet.add(rowCaptured.subtract(rowRefunded));
            }
        }

        PropertyCommissionCurrencyResponse toResponse(String currency, BigDecimal commissionPercent) {
            BigDecimal net = money(captured.subtract(refunded));
            boolean configured = commissionPercent != null;
            BigDecimal bhStaysRevenue = configured ? commission(base, commissionPercent) : null;
            BigDecimal ownerAmount = configured ? money(net.subtract(bhStaysRevenue)) : null;
            return new PropertyCommissionCurrencyResponse(
                    currency,
                    money(captured),
                    money(refunded),
                    net,
                    money(base),
                    configured ? money(commissionPercent) : null,
                    configured,
                    bhStaysRevenue,
                    ownerAmount,
                    money(unallocatedNet),
                    reservations);
        }
    }
}
