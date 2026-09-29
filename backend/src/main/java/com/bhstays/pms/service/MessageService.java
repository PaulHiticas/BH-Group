package com.bhstays.pms.service;

import com.bhstays.pms.common.exception.ResourceNotFoundException;
import com.bhstays.pms.domain.Message;
import com.bhstays.pms.domain.MessageSenderType;
import com.bhstays.pms.domain.NotificationType;
import com.bhstays.pms.domain.Reservation;
import com.bhstays.pms.domain.Role;
import com.bhstays.pms.domain.User;
import com.bhstays.pms.dto.messaging.MessageResponse;
import com.bhstays.pms.repository.MessageRepository;
import com.bhstays.pms.repository.ReservationRepository;
import com.bhstays.pms.repository.UserRepository;
import com.bhstays.pms.security.SecureTokenGenerator;
import com.bhstays.pms.service.mapper.MessageMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SecureTokenGenerator secureTokenGenerator;
    private final MessageMapper messageMapper;

    @Transactional(readOnly = true)
    public List<MessageResponse> listForReservation(UUID reservationId) {
        return messageRepository.findByReservationIdOrderByCreatedAtAsc(reservationId).stream()
                .map(messageMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listForReservationByToken(String token) {
        Reservation reservation = findByToken(token);
        return listForReservation(reservation.getId());
    }

    @Transactional
    public MessageResponse sendStaffMessage(UUID reservationId, UUID senderUserId, String body) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        User sender = userRepository.findById(senderUserId).orElse(null);

        Message message = Message.builder()
                .reservation(reservation)
                .senderType(MessageSenderType.STAFF)
                .senderUser(sender)
                .body(body)
                .readAt(Instant.now())
                .build();
        message = messageRepository.save(message);

        if (reservation.getGuestEmail() != null) {
            if (reservation.getManagementToken() == null) {
                reservation.setManagementToken(secureTokenGenerator.generateRawToken());
                reservationRepository.save(reservation);
            }
            emailService.sendNewMessageEmail(reservation.getGuestEmail(), reservation.getGuestFirstName(),
                    reservation.getProperty().getName(), body, reservation.getManagementToken());
        }

        return messageMapper.toResponse(message);
    }

    @Transactional
    public MessageResponse sendGuestMessage(String token, String body) {
        Reservation reservation = findByToken(token);

        Message message = Message.builder()
                .reservation(reservation)
                .senderType(MessageSenderType.GUEST)
                .body(body)
                .build();
        message = messageRepository.save(message);

        notificationService.notifyRoles(
                List.of(Role.SUPER_ADMIN, Role.ADMINISTRATOR, Role.SUPPORT_AGENT),
                NotificationType.NEW_MESSAGE,
                "Mesaj nou de la " + reservation.getGuestFullName(),
                body,
                "/dashboard/reservations/" + reservation.getId());

        return messageMapper.toResponse(message);
    }

    @Transactional
    public void markThreadReadByStaff(UUID reservationId) {
        messageRepository.markThreadReadForViewer(reservationId, MessageSenderType.STAFF, Instant.now());
    }

    @Transactional
    public void markThreadReadByGuest(String token) {
        Reservation reservation = findByToken(token);
        messageRepository.markThreadReadForViewer(reservation.getId(), MessageSenderType.GUEST, Instant.now());
    }

    private Reservation findByToken(String token) {
        return reservationRepository.findByManagementToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }
}
