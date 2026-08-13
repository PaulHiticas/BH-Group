package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.Message;
import com.bhgroup.pms.domain.MessageSenderType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByReservationIdOrderByCreatedAtAsc(UUID reservationId);

    @Modifying
    @Query("""
            update Message m set m.readAt = :now
            where m.reservation.id = :reservationId
              and m.senderType <> :viewerType
              and m.readAt is null
            """)
    void markThreadReadForViewer(@Param("reservationId") UUID reservationId,
                                  @Param("viewerType") MessageSenderType viewerType,
                                  @Param("now") Instant now);

    List<Message> findByReservationIdInOrderByCreatedAtAsc(List<UUID> reservationIds);

    /**
     * Redacts every message body (both guest and staff) for reservations
     * being GDPR-erased. Staff replies are included deliberately, not just
     * the guest's own messages - a staff reply can just as easily quote or
     * restate the guest's name, phone, or other details back into the
     * thread, so leaving it untouched would leave the same PII the
     * reservation fields just had cleared sitting in plain text one table
     * over.
     */
    @Modifying
    @Query("""
            update Message m set m.body = :redactedText
            where m.reservation.id in :reservationIds
            """)
    int redactMessagesForReservations(@Param("reservationIds") List<UUID> reservationIds,
                                       @Param("redactedText") String redactedText);
}
