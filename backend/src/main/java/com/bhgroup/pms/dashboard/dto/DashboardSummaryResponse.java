package com.bhgroup.pms.dashboard.dto;

import com.bhgroup.pms.lead.dto.LeadResponse;
import com.bhgroup.pms.reservation.dto.ReservationResponse;
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
