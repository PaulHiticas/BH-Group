package com.bhgroup.pms.service;

import java.math.BigDecimal;

/**
 * Prepared integration point for a real revenue-estimate data source (e.g.
 * a market pricing API or an internal model trained on historical bookings).
 *
 * No implementation is registered yet, and {@link LeadService} does not call
 * this at all - a REVENUE_ESTIMATE lead is currently answered by staff
 * manually, not by an automated calculation, since the previous hardcoded
 * per-city base price was never a real estimate. Wiring a real bean here is
 * a separate, explicitly approved future task.
 */
public interface RevenueEstimateProvider {

    Estimate estimate(String city, int bedrooms);

    record Estimate(BigDecimal estimatedNightlyRate, BigDecimal estimatedMonthlyRevenue, String currency) {
    }
}
