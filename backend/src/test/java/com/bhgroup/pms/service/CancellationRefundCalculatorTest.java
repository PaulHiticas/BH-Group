package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bhgroup.pms.domain.CancellationPolicy;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CancellationRefundCalculatorTest {

    private final CancellationRefundCalculator calculator = new CancellationRefundCalculator();

    @ParameterizedTest(name = "{0} at {1} days out -> {2}%")
    @CsvSource({
            // NON_REFUNDABLE: always zero, regardless of notice
            "NON_REFUNDABLE, 30, 0",
            "NON_REFUNDABLE, 1, 0",
            "NON_REFUNDABLE, 0, 0",

            // FLEXIBLE: full refund with >=1 day notice, nothing same-day
            "FLEXIBLE, 30, 100",
            "FLEXIBLE, 1, 100",
            "FLEXIBLE, 0, 0",

            // MODERATE: 100% at 5+, 50% at 1-4, 0% same-day
            "MODERATE, 5, 100",
            "MODERATE, 6, 100",
            "MODERATE, 4, 50",
            "MODERATE, 1, 50",
            "MODERATE, 0, 0",

            // STRICT: 100% at 14+, 50% at 7-13, 0% below
            "STRICT, 14, 100",
            "STRICT, 20, 100",
            "STRICT, 13, 50",
            "STRICT, 7, 50",
            "STRICT, 6, 0",
            "STRICT, 0, 0",
    })
    void refundPercentFor_matchesPolicyTiers(CancellationPolicy policy, long daysBeforeCheckIn, int expectedPercent) {
        BigDecimal result = calculator.refundPercentFor(policy, daysBeforeCheckIn);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(expectedPercent));
    }

    @ParameterizedTest(name = "{0} handles negative notice ({1} days, i.e. already past check-in)")
    @CsvSource({
            "NON_REFUNDABLE, -3, 0",
            "FLEXIBLE, -3, 0",
            "MODERATE, -3, 0",
            "STRICT, -3, 0",
    })
    void refundPercentFor_negativeDaysBehavesLikeSameDay(CancellationPolicy policy, long daysBeforeCheckIn,
                                                           int expectedPercent) {
        BigDecimal result = calculator.refundPercentFor(policy, daysBeforeCheckIn);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(expectedPercent));
    }
}
