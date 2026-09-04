package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.AssistantChatMessage;
import com.bhgroup.pms.domain.AssistantChatSenderType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssistantChatMessageRepository extends JpaRepository<AssistantChatMessage, UUID> {

    List<AssistantChatMessage> findByChatIdOrderByCreatedAtAsc(UUID chatId);

    List<AssistantChatMessage> findByChatIdInOrderByCreatedAtAsc(List<UUID> chatIds);

    @Modifying
    @Query("""
            update AssistantChatMessage m set m.readAt = :now
            where m.chat.id = :chatId
              and m.senderType = :senderType
              and m.readAt is null
            """)
    void markMessagesReadForSenderType(@Param("chatId") UUID chatId,
                                        @Param("senderType") AssistantChatSenderType senderType,
                                        @Param("now") Instant now);

    /**
     * GDPR erasure only redacts the visitor's own messages here - unlike
     * {@code MessageRepository.redactMessagesForReservations}, which also
     * redacts staff replies because those can restate the guest's details
     * back at them. AI/STAFF turns in an assistant handoff are the
     * assistant's or a staff member's own writing, not the guest's data.
     */
    @Modifying
    @Query("""
            update AssistantChatMessage m set m.body = :redactedText
            where m.chat.id in :chatIds
              and m.senderType = :senderType
            """)
    int redactMessagesForChats(@Param("chatIds") List<UUID> chatIds,
                                @Param("senderType") AssistantChatSenderType senderType,
                                @Param("redactedText") String redactedText);
}
