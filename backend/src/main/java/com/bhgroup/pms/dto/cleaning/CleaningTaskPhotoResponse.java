package com.bhgroup.pms.dto.cleaning;

import java.time.Instant;
import java.util.UUID;

public record CleaningTaskPhotoResponse(
        UUID id,
        String caption,
        Instant createdAt
) {
}
