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
}
