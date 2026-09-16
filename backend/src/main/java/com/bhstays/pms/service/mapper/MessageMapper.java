package com.bhstays.pms.service.mapper;

import com.bhstays.pms.domain.Message;
import com.bhstays.pms.domain.MessageSenderType;
import com.bhstays.pms.dto.messaging.MessageResponse;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message) {
        String senderName = message.getSenderType() == MessageSenderType.GUEST
                ? message.getReservation().getGuestFullName()
                : (message.getSenderUser() != null
                        ? message.getSenderUser().getFirstName() + " " + message.getSenderUser().getLastName()
                        : "Echipa BH Stays");
        return new MessageResponse(
                message.getId(), message.getSenderType(), senderName, message.getBody(),
                message.getReadAt(), message.getCreatedAt());
    }
}
