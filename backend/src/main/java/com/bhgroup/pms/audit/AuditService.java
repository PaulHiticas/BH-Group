package com.bhgroup.pms.audit;

import com.bhgroup.pms.user.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, User actor, String description, String ipAddress, String userAgent) {
        try {
            AuditLog log = AuditLog.builder()
                    .entityName("User")
                    .entityId(actor != null ? actor.getId().toString() : null)
                    .action(action.name())
                    .actorId(actor != null ? actor.getId() : null)
                    .actorEmail(actor != null ? actor.getEmail() : null)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .description(description)
                    .build();
            auditLogRepository.save(log);
        } catch (Exception ex) {
            log.error("Failed to persist audit log for action {}", action, ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordForUserId(AuditAction action, UUID actorId, String actorEmail, String description,
                                 String ipAddress, String userAgent) {
        try {
            AuditLog log = AuditLog.builder()
                    .entityName("User")
                    .entityId(actorId != null ? actorId.toString() : null)
                    .action(action.name())
                    .actorId(actorId)
                    .actorEmail(actorEmail)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .description(description)
                    .build();
            auditLogRepository.save(log);
        } catch (Exception ex) {
            log.error("Failed to persist audit log for action {}", action, ex);
        }
    }
}
