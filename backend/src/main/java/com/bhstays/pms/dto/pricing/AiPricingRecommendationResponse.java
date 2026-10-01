package com.bhstays.pms.dto.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A suggested dynamic-pricing configuration, with the numbers it was derived
 * from. Advisory only: nothing is written to the property's configuration by
 * asking for a recommendation - applying it is a separate, deliberate
 * {@code PUT /pricing/config} by the admin.
 */
public record AiPricingRecommendationResponse(
        UUID propertyId,
        String currency,
        PricingSignals signals,
        RecommendedConfig recommendation,
        String rationale,
        Instant generatedAt
) {

    /** What the model was shown - returned so a recommendation can be judged, not just trusted. */
    public record PricingSignals(
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
