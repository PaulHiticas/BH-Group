package com.bhgroup.pms.dto.dashboard;

import com.bhgroup.pms.dto.lead.LeadResponse;
import com.bhgroup.pms.dto.reservation.ReservationResponse;
import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
        long totalProperties,
        long totalReservations,
        BigDecimal totalRevenue,
        String currency,
        long uncontactedLeads,
        List<ReservationResponse> upcomingReservations,
        List<LeadResponse> recentLeads
) {
}
