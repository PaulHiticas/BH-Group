package com.bhstays.pms.dto.ical;

import com.bhstays.pms.domain.IcalSyncStatus;
import com.bhstays.pms.domain.ReservationSource;
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
