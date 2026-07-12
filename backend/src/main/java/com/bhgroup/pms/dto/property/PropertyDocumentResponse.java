package com.bhgroup.pms.dto.property;

import com.bhgroup.pms.domain.PropertyDocumentType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.bhgroup.pms.domain.PropertyDocumentType;
public record PropertyDocumentResponse(
        UUID id,
        String fileName,
        String url,
        PropertyDocumentType documentType,
        LocalDate expiresAt,
        Instant createdAt
) {
}
