package com.bhgroup.pms.dto.gdpr;

import java.time.Instant;
import java.util.List;

public record GdprExportResponse(
        String email,
        Instant exportedAt,
        List<GdprReservationExport> reservations,
        List<GdprLeadExport> leads
) {
}
