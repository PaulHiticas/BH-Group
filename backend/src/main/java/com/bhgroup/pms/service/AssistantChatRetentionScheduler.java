package com.bhgroup.pms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Once a day, deletes AI-assistant handoff chats (which can carry a
 * visitor's name, email, and messages) older than
 * {@code app.assistant.retention-days} ({@code ASSISTANT_CHAT_RETENTION_DAYS},
 * default 90). See {@link AssistantChatService#purgeOldChats()}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssistantChatRetentionScheduler {

    private final AssistantChatService assistantChatService;

    @Scheduled(cron = "0 15 7 * * *")
    public void purgeOldChats() {
        int purged = assistantChatService.purgeOldChats();
        if (purged > 0) {
            log.info("Assistant chat retention: purged {} chat(s) past the retention window", purged);
        }
    }
}
