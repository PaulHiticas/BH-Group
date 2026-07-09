package com.bhgroup.pms.lead;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyLeadRepository extends JpaRepository<PropertyLead, UUID> {

    Page<PropertyLead> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByContactedFalse();
}
