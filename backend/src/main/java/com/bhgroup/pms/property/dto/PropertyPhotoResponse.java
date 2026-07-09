package com.bhgroup.pms.property.dto;

import java.util.UUID;

public record PropertyPhotoResponse(
        UUID id,
        String url,
        String caption,
        int sortOrder,
        boolean cover
) {
}
