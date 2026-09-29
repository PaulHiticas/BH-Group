package com.bhstays.pms.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bhstays.pms.domain.PropertyPhoto;
public interface PropertyPhotoRepository extends JpaRepository<PropertyPhoto, UUID> {

    List<PropertyPhoto> findByPropertyIdOrderBySortOrderAsc(UUID propertyId);

    long countByPropertyId(UUID propertyId);
}
