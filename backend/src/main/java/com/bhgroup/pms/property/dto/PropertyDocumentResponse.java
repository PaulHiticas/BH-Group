package com.bhgroup.pms.property.dto;

import com.bhgroup.pms.property.PropertyDocumentType;
import java.time.Instant;
import java.util.UUID;

public record PropertyDocumentResponse(
        UUID id,
        String fileName,
        String url,
        PropertyDocumentType documentType,
        Instant createdAt
) {
}
