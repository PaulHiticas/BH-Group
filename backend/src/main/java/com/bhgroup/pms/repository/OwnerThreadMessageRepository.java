package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.OwnerThreadMessage;
import com.bhgroup.pms.domain.OwnerThreadSenderType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OwnerThreadMessageRepository extends JpaRepository<OwnerThreadMessage, UUID> {

    List<OwnerThreadMessage> findByThreadIdOrderByCreatedAtAsc(UUID threadId);

    @Modifying
    @Query("""
            update OwnerThreadMessage m set m.readAt = :now
            where m.thread.id = :threadId
              and m.senderType <> :viewerType
              and m.readAt is null
            """)
    void markThreadReadForViewer(@Param("threadId") UUID threadId,
                                  @Param("viewerType") OwnerThreadSenderType viewerType,
                                  @Param("now") Instant now);
}
