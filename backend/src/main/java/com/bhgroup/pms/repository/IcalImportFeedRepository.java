package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.IcalImportFeed;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IcalImportFeedRepository extends JpaRepository<IcalImportFeed, UUID> {

    List<IcalImportFeed> findByPropertyIdOrderByCreatedAtAsc(UUID propertyId);
}
