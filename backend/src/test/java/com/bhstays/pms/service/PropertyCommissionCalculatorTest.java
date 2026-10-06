package com.bhstays.pms.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bhstays.pms.dto.report.PropertyCommissionCurrencyResponse;
import com.bhstays.pms.repository.projection.ReservationPaymentTotals;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PropertyCommissionCalculatorTest {

    private static final UUID PROPERTY_ID = UUID.randomUUID();

    /** A reservation paid in its own currency, with a breakdown snapshot. */
    private static ReservationPaymentTotals row(String currency, String total, String accommodation,
                                                String captured, String refunded) {
        return new ReservationPaymentTotals(PROPERTY_ID, UUID.randomUUID(), currency, currency,
                total != null ? new BigDecimal(total) : null,
                accommodation != null ? new BigDecimal(accommodation) : null,
                new BigDecimal(captured), new BigDecimal(refunded));
    }

    private static PropertyCommissionCurrencyResponse single(String percent, ReservationPaymentTotals... rows) {
        List<PropertyCommissionCurrencyResponse> result = PropertyCommissionCalculator.calculate(
                percent != null ? new BigDecimal(percent) : null, List.of(rows));
        assertThat(result).hasSize(1);
        return result.get(0);
    }

    @Test
    void capturedPayment_isSplitIntoCommissionOnAccommodationAndOwnerAmount() {
        // 500 = 400 accommodation + 100 cleaning fee, fully paid, 20%
        PropertyCommissionCurrencyResponse line = single("20", row("RON", "500.00", "400.00", "500.00", "0"));

        assertThat(line.capturedTotal()).isEqualByComparingTo("500.00");
        assertThat(line.refundedTotal()).isEqualByComparingTo("0.00");
        assertThat(line.netRevenue()).isEqualByComparingTo("500.00");
        assertThat(line.commissionableBase()).isEqualByComparingTo("400.00");
        assertThat(line.bhStaysRevenue()).isEqualByComparingTo("80.00");
        assertThat(line.ownerAmount()).isEqualByComparingTo("420.00");
        assertThat(line.commissionConfigured()).isTrue();
        assertThat(line.paidReservationCount()).isEqualTo(1);
    }

    @Test
    void cleaningAndExtraGuestFees_areExcludedFromTheCommissionableBase() {
        // 1000 = 700 accommodation + 150 cleaning + 150 extra-guest fee, 10%
        PropertyCommissionCurrencyResponse line = single("10", row("RON", "1000.00", "700.00", "1000.00", "0"));

        assertThat(line.commissionableBase()).isEqualByComparingTo("700.00");
        assertThat(line.bhStaysRevenue()).isEqualByComparingTo("70.00");
        assertThat(line.ownerAmount()).isEqualByComparingTo("930.00");
    }

    @Test
    void fullRefund_leavesNothingToShare() {
        PropertyCommissionCurrencyResponse line = single("20", row("RON", "500.00", "400.00", "500.00", "500.00"));

        assertThat(line.capturedTotal()).isEqualByComparingTo("500.00");
        assertThat(line.refundedTotal()).isEqualByComparingTo("500.00");
        assertThat(line.netRevenue()).isEqualByComparingTo("0.00");
        assertThat(line.commissionableBase()).isEqualByComparingTo("0.00");
        assertThat(line.bhStaysRevenue()).isEqualByComparingTo("0.00");
        assertThat(line.ownerAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void partialRefund_reducesTheBaseProportionally() {
        // net 300 of 500 captured -> base = 400 * 300 / 500 = 240
        PropertyCommissionCurrencyResponse line = single("20", row("RON", "500.00", "400.00", "500.00", "200.00"));

        assertThat(line.netRevenue()).isEqualByComparingTo("300.00");
        assertThat(line.commissionableBase()).isEqualByComparingTo("240.00");
        assertThat(line.bhStaysRevenue()).isEqualByComparingTo("48.00");
        assertThat(line.ownerAmount()).isEqualByComparingTo("252.00");
    }

    @Test
    void zeroPercent_reportsNoBhStaysRevenueButIsConfigured() {
        PropertyCommissionCurrencyResponse line = single("0", row("RON", "500.00", "400.00", "500.00", "0"));

        assertThat(line.commissionConfigured()).isTrue();
        assertThat(line.bhStaysRevenue()).isEqualByComparingTo("0.00");
        assertThat(line.ownerAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void hundredPercent_keepsTheWholeAccommodationButNotTheFees() {
        PropertyCommissionCurrencyResponse line = single("100", row("RON", "500.00", "400.00", "500.00", "0"));

        assertThat(line.bhStaysRevenue()).isEqualByComparingTo("400.00");
        assertThat(line.ownerAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void unconfiguredPercent_reportsNoArtificialSplit() {
        PropertyCommissionCurrencyResponse line = single(null, row("RON", "500.00", "400.00", "500.00", "0"));

        assertThat(line.commissionConfigured()).isFalse();
        assertThat(line.commissionPercent()).isNull();
        assertThat(line.bhStaysRevenue()).isNull();
        assertThat(line.ownerAmount()).isNull();
        assertThat(line.netRevenue()).isEqualByComparingTo("500.00");
        assertThat(line.commissionableBase()).isEqualByComparingTo("400.00");
    }

    @Test
    void currencies_areNeverAddedTogether() {
        List<PropertyCommissionCurrencyResponse> result = PropertyCommissionCalculator.calculate(new BigDecimal("10"),
                List.of(row("RON", "500.00", "400.00", "500.00", "0"),
                        row("EUR", "100.00", "80.00", "100.00", "0"),
                        row("RON", "300.00", "300.00", "300.00", "0")));

        assertThat(result).extracting(PropertyCommissionCurrencyResponse::currency).containsExactly("EUR", "RON");
        PropertyCommissionCurrencyResponse eur = result.get(0);
        PropertyCommissionCurrencyResponse ron = result.get(1);
        assertThat(eur.netRevenue()).isEqualByComparingTo("100.00");
        assertThat(eur.bhStaysRevenue()).isEqualByComparingTo("8.00");
        assertThat(ron.netRevenue()).isEqualByComparingTo("800.00");
        assertThat(ron.commissionableBase()).isEqualByComparingTo("700.00");
        assertThat(ron.bhStaysRevenue()).isEqualByComparingTo("70.00");
        assertThat(ron.paidReservationCount()).isEqualTo(2);
    }

    @Test
    void rounding_isHalfUpToTwoDecimals() {
        // deposit of 100 on a 300 stay with 100 accommodation: base = 100 * 100 / 300 = 33.333.. -> 33.33
        // commission 15% of 33.33 = 4.9995 -> 5.00
        PropertyCommissionCurrencyResponse line = single("15", row("RON", "300.00", "100.00", "100.00", "0"));

        assertThat(line.commissionableBase()).isEqualByComparingTo("33.33");
        assertThat(line.bhStaysRevenue()).isEqualByComparingTo("5.00");
        assertThat(line.ownerAmount()).isEqualByComparingTo("95.00");
        assertThat(line.bhStaysRevenue().scale()).isEqualTo(2);
        assertThat(line.ownerAmount().scale()).isEqualTo(2);
        assertThat(line.netRevenue().scale()).isEqualTo(2);
        assertThat(line.commissionPercent().scale()).isEqualTo(2);
    }

    @Test
    void rounding_ofAFractionalPercent() {
        // 12.5% of 233.33 = 29.16625 -> 29.17
        PropertyCommissionCurrencyResponse line = single("12.5", row("RON", "333.33", "333.33", "333.33", "100.00"));

        assertThat(line.commissionableBase()).isEqualByComparingTo("233.33");
        assertThat(line.bhStaysRevenue()).isEqualByComparingTo("29.17");
        assertThat(line.ownerAmount()).isEqualByComparingTo("204.16");
    }

    @Test
    void reservationWithoutBreakdown_isReportedAsUnallocatedAndNotCommissioned() {
        PropertyCommissionCurrencyResponse line = single("20",
                row("RON", "500.00", "400.00", "500.00", "0"),
                row("RON", "250.00", null, "250.00", "50.00"));

        assertThat(line.netRevenue()).isEqualByComparingTo("700.00");
        assertThat(line.unallocatedNetRevenue()).isEqualByComparingTo("200.00");
        assertThat(line.commissionableBase()).isEqualByComparingTo("400.00");
        assertThat(line.bhStaysRevenue()).isEqualByComparingTo("80.00");
        assertThat(line.ownerAmount()).isEqualByComparingTo("620.00");
    }

    @Test
    void paymentInAnotherCurrencyThanTheReservation_isUnallocated() {
        ReservationPaymentTotals mismatched = new ReservationPaymentTotals(PROPERTY_ID, UUID.randomUUID(),
                "EUR", "RON", new BigDecimal("500.00"), new BigDecimal("400.00"),
                new BigDecimal("100.00"), BigDecimal.ZERO);

        PropertyCommissionCurrencyResponse line = single("20", mismatched);

        assertThat(line.currency()).isEqualTo("EUR");
        assertThat(line.commissionableBase()).isEqualByComparingTo("0.00");
        assertThat(line.unallocatedNetRevenue()).isEqualByComparingTo("100.00");
    }

    @Test
    void overpayment_neverCommissionsMoreThanTheAccommodation() {
        PropertyCommissionCurrencyResponse line = single("10", row("RON", "500.00", "400.00", "600.00", "0"));

        assertThat(line.commissionableBase()).isEqualByComparingTo("400.00");
        assertThat(line.bhStaysRevenue()).isEqualByComparingTo("40.00");
        assertThat(line.ownerAmount()).isEqualByComparingTo("560.00");
    }

    @Test
    void noRows_meansNoCurrencyLines() {
        assertThat(PropertyCommissionCalculator.calculate(new BigDecimal("20"), List.of())).isEmpty();
    }
}
