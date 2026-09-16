package com.bhstays.pms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bhstays.pms.domain.PropertyDocument;
public interface PropertyDocumentRepository extends JpaRepository<PropertyDocument, UUID> {

    List<PropertyDocument> findByPropertyIdOrderByCreatedAtDesc(UUID propertyId);

    List<PropertyDocument> findByExpiresAtLessThanEqualAndExpiryNotifiedAtIsNull(LocalDate cutoff);
}
