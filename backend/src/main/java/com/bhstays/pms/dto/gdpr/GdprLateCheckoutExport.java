package com.bhstays.pms.dto.gdpr;

import com.bhstays.pms.domain.LateCheckoutStatus;
import java.time.Instant;

public record GdprLateCheckoutExport(
        LateCheckoutStatus status,
        String guestNote,
        Instant createdAt
) {
}
