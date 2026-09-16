package com.bhstays.pms.dto.owner;

import com.bhstays.pms.dto.maintenance.MaintenanceTicketResponse;
import com.bhstays.pms.dto.reservation.ReservationResponse;
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
