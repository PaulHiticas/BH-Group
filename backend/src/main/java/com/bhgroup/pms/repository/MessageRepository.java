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

    /** Redacts guest-authored message bodies for reservations being GDPR-erased; staff replies are kept for context. */
    @Modifying
    @Query("""
            update Message m set m.body = :redactedText
            where m.reservation.id in :reservationIds
              and m.senderType = com.bhgroup.pms.domain.MessageSenderType.GUEST
            """)
    int redactGuestMessagesForReservations(@Param("reservationIds") List<UUID> reservationIds,
                                            @Param("redactedText") String redactedText);
}
