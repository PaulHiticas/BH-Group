package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.domain.OwnerThread;
import com.bhgroup.pms.domain.OwnerThreadMessage;
import com.bhgroup.pms.domain.OwnerThreadSenderType;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadDetailResponse;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadMessageResponse;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OwnerThreadMapper {

    public OwnerThreadSummaryResponse toSummaryResponse(OwnerThread thread) {
        return new OwnerThreadSummaryResponse(
                thread.getId(),
                thread.getSubject(),
                thread.getStatus(),
                thread.getProperty() != null ? thread.getProperty().getId() : null,
                thread.getProperty() != null ? thread.getProperty().getName() : null,
                thread.getOwner().getId(),
                thread.getOwner().getFullName(),
                thread.getLastMessageAt(),
                thread.getCreatedAt());
    }

    public OwnerThreadDetailResponse toDetailResponse(OwnerThread thread, List<OwnerThreadMessage> messages) {
        return new OwnerThreadDetailResponse(
                thread.getId(),
                thread.getSubject(),
                thread.getStatus(),
                thread.getProperty() != null ? thread.getProperty().getId() : null,
                thread.getProperty() != null ? thread.getProperty().getName() : null,
                thread.getOwner().getId(),
                thread.getOwner().getFullName(),
                thread.getLastMessageAt(),
                thread.getCreatedAt(),
                messages.stream().map(this::toMessageResponse).toList());
    }

    public OwnerThreadMessageResponse toMessageResponse(OwnerThreadMessage message) {
        String senderName = message.getSenderType() == OwnerThreadSenderType.OWNER
                ? message.getThread().getOwner().getFullName()
                : (message.getSenderUser() != null ? message.getSenderUser().getFullName() : "Echipa BH Group");
        return new OwnerThreadMessageResponse(
                message.getId(), message.getSenderType(), senderName, message.getBody(),
                message.getReadAt(), message.getCreatedAt());
    }
}
