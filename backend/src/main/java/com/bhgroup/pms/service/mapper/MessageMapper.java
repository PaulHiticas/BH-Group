package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.domain.Message;
import com.bhgroup.pms.domain.MessageSenderType;
import com.bhgroup.pms.dto.messaging.MessageResponse;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message) {
        String senderName = message.getSenderType() == MessageSenderType.GUEST
                ? message.getReservation().getGuestFullName()
                : (message.getSenderUser() != null
                        ? message.getSenderUser().getFirstName() + " " + message.getSenderUser().getLastName()
                        : "Echipa BH Group");
        return new MessageResponse(
                message.getId(), message.getSenderType(), senderName, message.getBody(),
                message.getReadAt(), message.getCreatedAt());
    }
}
