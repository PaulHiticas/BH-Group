package com.bhstays.pms.dto.owner;

import com.bhstays.pms.dto.maintenance.MaintenanceTicketResponse;
import com.bhstays.pms.dto.reservation.ReservationResponse;
import java.math.BigDecimal;
import java.util.List;

/**
 * {@code revenueByCurrency} holds the owner's figures, one line per
 * currency, on the same formula as the statements. The flat amount fields
 * are deprecated: they only ever covered RON (they used to add every
 * currency together under a RON label) and are kept for API compatibility.
 */
public record OwnerDashboardSummaryResponse(
        int totalProperties,
        /* Deprecated: RON net collected revenue; use {@code revenueByCurrency}. */
        @Deprecated BigDecimal grossRevenue,
        /* Deprecated: RON BH Stays commission (0 if not computable); use {@code revenueByCurrency}. */
        @Deprecated BigDecimal commissionAmount,
        /* Deprecated: RON owner-chargeable expenses; use {@code revenueByCurrency}. */
        @Deprecated BigDecimal expensesTotal,
        /* Deprecated: RON payout; use {@code revenueByCurrency}. */
        @Deprecated BigDecimal netRevenue,
        /* Deprecated: always RON. */
        @Deprecated String currency,
        List<ReservationResponse> upcomingReservations,
        List<MaintenanceTicketResponse> openMaintenanceTickets,
        List<OwnerRevenueLine> revenueByCurrency
) {
}
