package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.AssistantChat;
import com.bhgroup.pms.domain.AssistantChatStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantChatRepository extends JpaRepository<AssistantChat, UUID> {

    Optional<AssistantChat> findByPublicToken(String publicToken);

    Page<AssistantChat> findByStatus(AssistantChatStatus status, Pageable pageable);
}
