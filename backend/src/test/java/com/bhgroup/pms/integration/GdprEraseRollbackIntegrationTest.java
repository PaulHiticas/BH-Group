package com.bhgroup.pms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.domain.Address;
import com.bhgroup.pms.domain.AuditAction;
import com.bhgroup.pms.domain.GdprVerificationMethod;
import com.bhgroup.pms.domain.LateCheckoutRequest;
import com.bhgroup.pms.domain.Message;
import com.bhgroup.pms.domain.MessageSenderType;
import com.bhgroup.pms.domain.Notification;
import com.bhgroup.pms.domain.NotificationType;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyLead;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.PropertyType;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.domain.UserStatus;
import com.bhgroup.pms.repository.AuditLogRepository;
import com.bhgroup.pms.repository.GdprRequestRepository;
import com.bhgroup.pms.repository.LateCheckoutRequestRepository;
import com.bhgroup.pms.repository.MessageRepository;
import com.bhgroup.pms.repository.NotificationRepository;
import com.bhgroup.pms.repository.PropertyLeadRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.GdprService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * A mocked-repository unit test can prove the anonymization logic is
 * correct, but it can't prove @Transactional actually rolls back a
 * half-applied change in a real database - that requires a real
 * Postgres transaction to genuinely commit or abort, and it can't prove
 * the deferred (afterCommit) audit write really stays silent on failure
 * either, since that behavior only exists at the real transaction-
 * synchronization level Mockito doesn't simulate.
 *
 * Forces {@link GdprService#erase} to fail at the very last write in the
 * method - persisting the {@code gdpr_requests} compliance row, which
 * happens after the reservation, lead, message, notification, and late
 * checkout request writes have already been applied in this same
 * transaction - then re-reads every one of them fresh (a completely
 * separate read, not the managed entity still held in memory) to confirm
 * nothing survived, and confirms no audit log entry exists either.
 */
class GdprEraseRollbackIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private GdprService gdprService;
    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private PropertyLeadRepository propertyLeadRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private LateCheckoutRequestRepository lateCheckoutRequestRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @MockBean
    private GdprRequestRepository gdprRequestRepository;

    private static final String GUEST_EMAIL = "rollback-test@example.com";

    @Test
    void erase_rollsBackEveryWriteAndLeavesNoAuditTraceWhenTheLastStepFails() {
        User staffUser = userRepository.save(User.builder()
                .email("staff-rollback-test@bhgroup.io")
                .passwordHash("irrelevant-for-this-test")
                .firstName("Staff")
                .lastName("Member")
                .role(Role.ADMINISTRATOR)
                .status(UserStatus.ACTIVE)
                .build());

        Property property = propertyRepository.save(Property.builder()
                .name("Rollback test property")
                .propertyType(PropertyType.APARTMENT)
                .status(PropertyStatus.ACTIVE)
                .address(new Address("Str. Test 1", "Cluj-Napoca", null, null, "România", null, null))
                .bedrooms(1)
                .bathrooms(1)
                .maxGuests(2)
                .build());

        Reservation reservation = reservationRepository.saveAndFlush(Reservation.builder()
                .property(property)
                .guestFirstName("Ion")
                .guestLastName("Popescu")
                .guestEmail(GUEST_EMAIL)
                .guestPhone("0722000000")
                .notes("Ajunge seara")
                .accessCode("1234")
                .managementToken("still-live-token-" + UUID.randomUUID())
                .checkInDate(LocalDate.of(2027, 9, 1))
                .checkOutDate(LocalDate.of(2027, 9, 5))
                .numberOfGuests(2)
                .status(ReservationStatus.CONFIRMED)
                .build());
        UUID reservationId = reservation.getId();

        Message guestMessage = messageRepository.saveAndFlush(Message.builder()
                .reservation(reservation)
                .senderType(MessageSenderType.GUEST)
                .body("Pot ajunge mai devreme?")
                .build());
        Message staffMessage = messageRepository.saveAndFlush(Message.builder()
                .reservation(reservation)
                .senderType(MessageSenderType.STAFF)
                .senderUser(staffUser)
                .body("Da, Ion, te așteptăm de la ora 14.")
                .build());

        Notification notification = notificationRepository.save(Notification.builder()
                .user(staffUser)
                .type(NotificationType.NEW_MESSAGE)
                .title("Mesaj nou de la Ion Popescu")
                .body("Pot ajunge mai devreme?")
                .linkPath("/dashboard/reservations/" + reservationId)
                .build());

        PropertyLead lead = propertyLeadRepository.save(PropertyLead.builder()
                .fullName("Ion Popescu")
                .email(GUEST_EMAIL)
                .phone("0722000000")
                .message("Vreau să știu mai multe")
                .build());

        LateCheckoutRequest lateCheckoutRequest = lateCheckoutRequestRepository.save(LateCheckoutRequest.builder()
                .reservation(reservation)
                .requestedCheckoutTime(LocalTime.of(14, 0))
                .guestNote("Sunt Ion Popescu, sunați la 0722000000 dacă e nevoie")
                .build());

        long auditLogCountBefore = auditLogRepository.count();

        doThrow(new RuntimeException("simulated failure on the last write of the transaction"))
                .when(gdprRequestRepository).save(any());

        User actor = new User();
        actor.setId(UUID.randomUUID());

        assertThatThrownBy(() -> gdprService.erase(
                GUEST_EMAIL, GdprVerificationMethod.RESERVATION_DETAILS, "Verificat telefonic", actor))
                .isInstanceOf(RuntimeException.class);

        Reservation rereadReservation = reservationRepository.findById(reservationId).orElseThrow();
        assertThat(rereadReservation.getGuestEmail()).isEqualTo(GUEST_EMAIL);
        assertThat(rereadReservation.getGuestFirstName()).isEqualTo("Ion");
        assertThat(rereadReservation.getGuestLastName()).isEqualTo("Popescu");
        assertThat(rereadReservation.getGuestPhone()).isEqualTo("0722000000");
        assertThat(rereadReservation.getNotes()).isEqualTo("Ajunge seara");
        assertThat(rereadReservation.getAccessCode()).isEqualTo("1234");
        assertThat(rereadReservation.getManagementToken()).isEqualTo(reservation.getManagementToken());

        PropertyLead rereadLead = propertyLeadRepository.findById(lead.getId()).orElseThrow();
        assertThat(rereadLead.getEmail()).isEqualTo(GUEST_EMAIL);
        assertThat(rereadLead.getFullName()).isEqualTo("Ion Popescu");

        Message rereadGuestMessage = messageRepository.findById(guestMessage.getId()).orElseThrow();
        Message rereadStaffMessage = messageRepository.findById(staffMessage.getId()).orElseThrow();
        assertThat(rereadGuestMessage.getBody()).isEqualTo("Pot ajunge mai devreme?");
        assertThat(rereadStaffMessage.getBody()).isEqualTo("Da, Ion, te așteptăm de la ora 14.");

        Notification rereadNotification = notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(rereadNotification.getTitle()).isEqualTo("Mesaj nou de la Ion Popescu");
        assertThat(rereadNotification.getBody()).isEqualTo("Pot ajunge mai devreme?");

        LateCheckoutRequest rereadLateCheckoutRequest =
                lateCheckoutRequestRepository.findById(lateCheckoutRequest.getId()).orElseThrow();
        assertThat(rereadLateCheckoutRequest.getGuestNote())
                .isEqualTo("Sunt Ion Popescu, sunați la 0722000000 dacă e nevoie");

        // the audit write is deferred until after commit - a rolled-back
        // transaction must never leave an audit trail claiming success.
        assertThat(auditLogRepository.count()).isEqualTo(auditLogCountBefore);
        assertThat(auditLogRepository.findAll().stream())
                .noneMatch(log -> AuditAction.GDPR_DATA_ERASED.name().equals(log.getAction()));
    }
}
