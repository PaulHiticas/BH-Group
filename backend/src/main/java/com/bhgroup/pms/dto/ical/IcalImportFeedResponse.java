package com.bhgroup.pms.dto.ical;

import com.bhgroup.pms.domain.IcalSyncStatus;
import com.bhgroup.pms.domain.ReservationSource;
import java.time.Instant;
import java.util.UUID;

public record IcalImportFeedResponse(
        UUID id,
        ReservationSource source,
        String feedUrl,
        Instant lastSyncedAt,
        IcalSyncStatus lastSyncStatus,
        String lastSyncError
) {
}
