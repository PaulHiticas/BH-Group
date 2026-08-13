package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.GdprRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GdprRequestRepository extends JpaRepository<GdprRequest, UUID> {
}
