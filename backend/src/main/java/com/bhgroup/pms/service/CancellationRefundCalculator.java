package com.bhgroup.pms.service;

import com.bhgroup.pms.domain.CancellationPolicy;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Turns a property's cancellation policy + how many days before check-in a
 * guest cancels into a refund percentage. Tiers follow common industry
 * convention (similar to Airbnb's flexible/moderate/strict):
 *
 * <ul>
 *   <li>FLEXIBLE — full refund up to 1 day before check-in</li>
 *   <li>MODERATE — full refund 5+ days out, 50% from 1-4 days, none after</li>
 *   <li>STRICT — full refund 14+ days out, 50% from 7-13 days, none after</li>
 *   <li>NON_REFUNDABLE — never</li>
 * </ul>
 */
@Component
public class CancellationRefundCalculator {

    public BigDecimal refundPercentFor(CancellationPolicy policy, long daysBeforeCheckIn) {
        return switch (policy) {
            case NON_REFUNDABLE -> BigDecimal.ZERO;
            case FLEXIBLE -> daysBeforeCheckIn >= 1 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
            case MODERATE -> {
                if (daysBeforeCheckIn >= 5) yield BigDecimal.valueOf(100);
                if (daysBeforeCheckIn >= 1) yield BigDecimal.valueOf(50);
                yield BigDecimal.ZERO;
            }
            case STRICT -> {
                if (daysBeforeCheckIn >= 14) yield BigDecimal.valueOf(100);
                if (daysBeforeCheckIn >= 7) yield BigDecimal.valueOf(50);
                yield BigDecimal.ZERO;
            }
        };
    }
}
