package com.bhgroup.pms.property;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyPhotoRepository extends JpaRepository<PropertyPhoto, UUID> {

    List<PropertyPhoto> findByPropertyIdOrderBySortOrderAsc(UUID propertyId);

    long countByPropertyId(UUID propertyId);
}
