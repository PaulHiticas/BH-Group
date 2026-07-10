package com.bhgroup.pms.dto.property;

import java.util.UUID;

public record PropertyPhotoResponse(
        UUID id,
        String url,
        String caption,
        int sortOrder,
        boolean cover
) {
}
