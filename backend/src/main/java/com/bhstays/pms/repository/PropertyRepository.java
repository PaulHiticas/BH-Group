package com.bhstays.pms.repository;

import com.bhstays.pms.repository.projection.PropertyCommissionSettings;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bhstays.pms.domain.Property;
import com.bhstays.pms.domain.PropertyStatus;
public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

    long countByStatus(PropertyStatus status);

    Page<Property> findByOwnerId(UUID ownerId, Pageable pageable);

    List<Property> findByOwnerId(UUID ownerId);

    @Query("""
            select new com.bhstays.pms.repository.projection.PropertyCommissionSettings(p.id, p.name, p.commissionPercent)
            from Property p
            order by p.name
            """)
    List<PropertyCommissionSettings> findAllCommissionSettings();

    @Query("""
            select new com.bhstays.pms.repository.projection.PropertyCommissionSettings(p.id, p.name, p.commissionPercent)
            from Property p
            where p.id = :id
            """)
    Optional<PropertyCommissionSettings> findCommissionSettings(@Param("id") UUID id);
}
