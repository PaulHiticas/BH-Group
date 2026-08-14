package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.LateCheckoutRequest;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LateCheckoutRequestRepository extends JpaRepository<LateCheckoutRequest, UUID> {

    Optional<LateCheckoutRequest> findByReservationId(UUID reservationId);

    List<LateCheckoutRequest> findByReservationIdIn(List<UUID> reservationIds);

    /**
     * Row-level lock for approve/reject/markPaid: without it, two concurrent
     * decisions on the same request (e.g. a double-click, or two staff
     * members acting at once) can both read status=REQUESTED before either
     * writes, and both "win".
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LateCheckoutRequest r where r.id = :id")
    Optional<LateCheckoutRequest> findByIdForUpdate(@Param("id") UUID id);

    /**
     * guestNote is free text a guest submits directly (including through the
     * unauthenticated management-token link) - nothing stops them writing
     * their own name, phone number, or other identifying detail into it, so
     * GDPR erasure has to clear it too, not just the Reservation fields it
     * was requested against.
     */
    @Modifying
    @Query("update LateCheckoutRequest r set r.guestNote = null "
            + "where r.reservation.id in :reservationIds and r.guestNote is not null")
    int redactGuestNoteForReservations(@Param("reservationIds") List<UUID> reservationIds);
}
