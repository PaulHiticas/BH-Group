package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.LateCheckoutRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LateCheckoutRequestRepository extends JpaRepository<LateCheckoutRequest, UUID> {

    Optional<LateCheckoutRequest> findByReservationId(UUID reservationId);

    /**
     * Row-level lock for approve/reject/markPaid: without it, two concurrent
     * decisions on the same request (e.g. a double-click, or two staff
     * members acting at once) can both read status=REQUESTED before either
     * writes, and both "win".
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LateCheckoutRequest r where r.id = :id")
    Optional<LateCheckoutRequest> findByIdForUpdate(@Param("id") UUID id);
}
