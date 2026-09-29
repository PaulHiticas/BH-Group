package com.bhstays.pms.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.bhstays.pms.domain.Property;
import com.bhstays.pms.domain.PropertyStatus;
public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

    long countByStatus(PropertyStatus status);

    Page<Property> findByOwnerId(UUID ownerId, Pageable pageable);

    List<Property> findByOwnerId(UUID ownerId);
}
