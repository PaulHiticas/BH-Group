package com.bhgroup.pms.dto.owner;

import com.bhgroup.pms.dto.maintenance.MaintenanceTicketResponse;
import com.bhgroup.pms.dto.reservation.ReservationResponse;
import java.math.BigDecimal;
import java.util.List;

public record OwnerDashboardSummaryResponse(
        int totalProperties,
        BigDecimal grossRevenue,
        BigDecimal commissionAmount,
        BigDecimal expensesTotal,
        BigDecimal netRevenue,
        String currency,
        List<ReservationResponse> upcomingReservations,
        List<MaintenanceTicketResponse> openMaintenanceTickets
) {
}
