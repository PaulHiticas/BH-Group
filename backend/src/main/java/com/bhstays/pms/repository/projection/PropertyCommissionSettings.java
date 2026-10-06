package com.bhstays.pms.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Just what the commission report needs from a property - no eager collections loaded. */
public record PropertyCommissionSettings(UUID id, String name, BigDecimal commissionPercent) {
}
