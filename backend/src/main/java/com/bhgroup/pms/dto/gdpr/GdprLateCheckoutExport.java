package com.bhgroup.pms.dto.gdpr;

import com.bhgroup.pms.domain.LateCheckoutStatus;
import java.time.Instant;

public record GdprLateCheckoutExport(
        LateCheckoutStatus status,
        String guestNote,
        Instant createdAt
) {
}
