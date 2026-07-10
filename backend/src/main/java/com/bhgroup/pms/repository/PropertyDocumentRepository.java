package com.bhgroup.pms.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bhgroup.pms.domain.PropertyDocument;
public interface PropertyDocumentRepository extends JpaRepository<PropertyDocument, UUID> {

    List<PropertyDocument> findByPropertyIdOrderByCreatedAtDesc(UUID propertyId);
}
