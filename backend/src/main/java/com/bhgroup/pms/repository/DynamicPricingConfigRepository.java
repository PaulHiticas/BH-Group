package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.DynamicPricingConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DynamicPricingConfigRepository extends JpaRepository<DynamicPricingConfig, UUID> {

    Optional<DynamicPricingConfig> findByPropertyId(UUID propertyId);
}
