package com.bhstays.pms.dto.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A suggested dynamic-pricing configuration, with everything needed to judge
 * it rather than just trust it: the metrics behind it, why the model landed
 * there, and what it could not see.
 *
 * <p>Advisory only: asking for a recommendation writes nothing. Applying it
 * stays a deliberate {@code PUT /pricing/config} by the admin.
 */
public record AiPricingRecommendationResponse(
        UUID propertyId,
        String currency,
        RecommendedConfig recommendation,
        Confidence confidence,
        String summary,
        List<PricingReason> reasons,
        PricingMetrics metricsUsed,
        List<String> warnings,
        List<String> missingData,
        Instant generatedAt
) {

    /**
     * How much the numbers behind this are worth leaning on. Derived from the
     * data actually available, not claimed by the model - the server knows
     * how thin the history is, the model only knows what it was handed.
     */
    public enum Confidence {
        LOW,
        MEDIUM,
        HIGH
    }

    /** Closed vocabulary accepted from the model. Every returned code is re-validated against server data. */
    public enum ReasonCode {
        LOW_FUTURE_OCCUPANCY,
        HIGH_FUTURE_OCCUPANCY,
        BELOW_HISTORICAL_OCCUPANCY,
        ABOVE_HISTORICAL_OCCUPANCY,
        SHORT_LEAD_TIME,
        LONG_LEAD_TIME,
        LOCAL_EVENT_CONFIGURED,
        INSUFFICIENT_HISTORY,
        WEEKEND_DEMAND_PATTERN
    }

    /** Romanian copy and the exact evidence used to justify one accepted reason code. */
    public record PricingReason(
            ReasonCode code,
            String message,
            String indicator,
            BigDecimal currentValue,
            BigDecimal comparisonValue,
            int periodDays
    ) {
    }

    /** What the model was shown. */
    public record PricingMetrics(
            int windowDays,
            int bookedNights,
            int windowNights,
            /** Booked share of the window, 0.00-1.00. */
            BigDecimal occupancyRate,
            /** Average nightly rate actually achieved in the window, or null with nothing booked. */
            BigDecimal averageDailyRate,
            BigDecimal basePricePerNight,
            int seasonalRatesConfigured,
            int upcomingLocalEvents
    ) {
    }

    /**
     * Deliberately field-for-field identical to
     * {@link DynamicPricingConfigUpdateRequest}, so an accepted recommendation
     * can be sent straight back to the update endpoint unchanged.
     */
    public record RecommendedConfig(
            boolean enabled,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer occupancyWindowDays,
            BigDecimal occupancyMultiplierMin,
            BigDecimal occupancyMultiplierMax,
            Integer leadTimeDays,
            BigDecimal leadTimeMultiplier
    ) {
    }
}
