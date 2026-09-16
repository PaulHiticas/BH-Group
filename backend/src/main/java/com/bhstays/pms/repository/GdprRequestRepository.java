package com.bhstays.pms.repository;

import com.bhstays.pms.domain.GdprRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GdprRequestRepository extends JpaRepository<GdprRequest, UUID> {
}
