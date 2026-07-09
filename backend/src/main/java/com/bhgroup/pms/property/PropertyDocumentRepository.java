package com.bhgroup.pms.property;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyDocumentRepository extends JpaRepository<PropertyDocument, UUID> {

    List<PropertyDocument> findByPropertyIdOrderByCreatedAtDesc(UUID propertyId);
}
