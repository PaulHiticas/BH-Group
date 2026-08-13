package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.LateCheckoutRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LateCheckoutRequestRepository extends JpaRepository<LateCheckoutRequest, UUID> {

    Optional<LateCheckoutRequest> findByReservationId(UUID reservationId);
}
