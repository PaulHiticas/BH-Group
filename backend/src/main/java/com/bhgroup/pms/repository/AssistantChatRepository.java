package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.AssistantChat;
import com.bhgroup.pms.domain.AssistantChatStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantChatRepository extends JpaRepository<AssistantChat, UUID> {

    Optional<AssistantChat> findByPublicToken(String publicToken);

    Page<AssistantChat> findByStatus(AssistantChatStatus status, Pageable pageable);

    List<AssistantChat> findByGuestEmailIgnoreCase(String guestEmail);

    /** Retention: chats (any status) that haven't had activity since the cutoff. */
    List<AssistantChat> findByLastMessageAtBefore(Instant cutoff);
}
