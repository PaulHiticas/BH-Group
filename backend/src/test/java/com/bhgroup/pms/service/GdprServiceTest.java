package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.config.AppProperties;
import com.bhgroup.pms.domain.AssistantChat;
import com.bhgroup.pms.domain.AssistantChatMessage;
import com.bhgroup.pms.domain.AssistantChatSenderType;
import com.bhgroup.pms.domain.AssistantChatStatus;
import com.bhgroup.pms.domain.AuditAction;
import com.bhgroup.pms.domain.GdprRecordType;
import com.bhgroup.pms.domain.GdprRequestType;
import com.bhgroup.pms.domain.GdprVerificationMethod;
import com.bhgroup.pms.domain.LateCheckoutRequest;
import com.bhgroup.pms.domain.LateCheckoutStatus;
import com.bhgroup.pms.domain.Message;
import com.bhgroup.pms.domain.MessageSenderType;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyLead;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationSource;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.repository.AssistantChatMessageRepository;
import com.bhgroup.pms.repository.AssistantChatRepository;
import com.bhgroup.pms.repository.GdprRequestRepository;
import com.bhgroup.pms.repository.LateCheckoutRequestRepository;
import com.bhgroup.pms.repository.MessageRepository;
import com.bhgroup.pms.repository.NotificationRepository;
import com.bhgroup.pms.repository.PropertyLeadRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GdprServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private PropertyLeadRepository propertyLeadRepository;
    @Mock private AssistantChatRepository assistantChatRepository;
    @Mock private AssistantChatMessageRepository assistantChatMessageRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private LateCheckoutRequestRepository lateCheckoutRequestRepository;
    @Mock private GdprRequestRepository gdprRequestRepository;
    @Mock private AuditService auditService;

    private GdprService gdprService;
    private User actor;
    private final String email = "guest@example.com";
    private final GdprVerificationMethod method = GdprVerificationMethod.RESERVATION_DETAILS;
    private final String note = "Confirmat data check-in și numele proprietății la telefon";

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("test-secret-not-for-production");

        gdprService = new GdprService(reservationRepository, propertyLeadRepository, assistantChatRepository,
                assistantChatMessageRepository, messageRepository, notificationRepository,
                lateCheckoutRequestRepository, gdprRequestRepository, auditService, appProperties);
        actor = new User();
        actor.setId(UUID.randomUUID());
    }

    private AssistantChat buildAssistantChat() {
        AssistantChat chat = AssistantChat.builder()
                .publicToken("tok-" + UUID.randomUUID())
                .guestName("Ion Popescu")
                .guestEmail(email)
                .status(AssistantChatStatus.RESOLVED)
                .build();
        chat.setId(UUID.randomUUID());
        return chat;
    }

    private Reservation buildReservation() {
        Property property = Property.builder().name("Casa Mare").build();
        property.setId(UUID.randomUUID());
        Reservation reservation = Reservation.builder()
                .property(property)
                .guestFirstName("Ion")
                .guestLastName("Popescu")
                .guestEmail(email)
                .guestPhone("0722000000")
                .checkInDate(LocalDate.of(2026, 7, 1))
                .checkOutDate(LocalDate.of(2026, 7, 5))
                .status(ReservationStatus.CONFIRMED)
                .source(ReservationSource.DIRECT)
                .notes("Ajunge seara")
                .accessCode("1234")
                .managementToken("abc123-still-live-link")
                .totalAmount(new java.math.BigDecimal("500.00"))
                .currency("RON")
                .build();
        reservation.setId(UUID.randomUUID());
        return reservation;
    }

    @Test
    void search_combinesReservationAndLeadMatches() {
        Reservation reservation = buildReservation();
        PropertyLead lead = PropertyLead.builder().fullName("Ion Popescu").email(email).city("Cluj").build();
        lead.setId(UUID.randomUUID());

        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of(reservation));
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of(lead));

        var results = gdprService.search(email);

        assertThat(results).hasSize(2);
    }

    @Test
    void search_includesAssistantChatMatches() {
        AssistantChat chat = buildAssistantChat();
        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of());
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of());
        when(assistantChatRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of(chat));

        var results = gdprService.search(email);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).recordType()).isEqualTo(GdprRecordType.ASSISTANT_CHAT);
        assertThat(results.get(0).name()).isEqualTo("Ion Popescu");
        assertThat(results.get(0).email()).isEqualTo(email);
    }

    @Test
    void erase_anonymizesReservationAndLeadRedactsMessagesAndRevokesManagementToken() {
        Reservation reservation = buildReservation();
        PropertyLead lead = PropertyLead.builder().fullName("Ion Popescu").email(email).phone("0722").message("Vreau info").build();
        lead.setId(UUID.randomUUID());

        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of(reservation));
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of(lead));
        when(messageRepository.redactMessagesForReservations(any(), any())).thenReturn(3);
        when(lateCheckoutRequestRepository.redactGuestNoteForReservations(any())).thenReturn(1);
        when(reservationRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(propertyLeadRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = gdprService.erase(email, method, note, actor);

        assertThat(result.reservationsErased()).isEqualTo(1);
        assertThat(result.leadsErased()).isEqualTo(1);
        assertThat(result.messagesRedacted()).isEqualTo(3);
        assertThat(result.lateCheckoutNotesRedacted()).isEqualTo(1);

        assertThat(reservation.getGuestEmail()).isNull();
        assertThat(reservation.getGuestPhone()).isNull();
        assertThat(reservation.getNotes()).isNull();
        assertThat(reservation.getAccessCode()).isNull();
        assertThat(reservation.getGuestFirstName()).isEqualTo("Șters");
        // the public self-service link must die with the guest's data - otherwise
        // whoever still has the old link can keep viewing/modifying/messaging.
        assertThat(reservation.getManagementToken()).isNull();
        // fiscally-relevant fields must survive erasure
        assertThat(reservation.getTotalAmount()).isEqualByComparingTo("500.00");
        assertThat(reservation.getCheckInDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(reservation.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(reservation.getCurrency()).isEqualTo("RON");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getProperty()).isNotNull();

        assertThat(lead.getEmail()).isNull();
        assertThat(lead.getPhone()).isNull();
        assertThat(lead.getMessage()).isNull();

        verify(notificationRepository).redactByLinkPaths(
                eq(List.of("/dashboard/reservations/" + reservation.getId())), any(), any());
        verify(lateCheckoutRequestRepository).redactGuestNoteForReservations(
                eq(List.of(reservation.getId())));

        verify(gdprRequestRepository).save(argThat(req ->
                req.getRequestType() == GdprRequestType.ERASE
                        && req.getRecordsAffected() == 2
                        && req.getMaskedEmail().equals("g***@example.com")
                        && !req.getMaskedEmail().equals(email)
                        && req.getEmailFingerprint() != null
                        && !req.getEmailFingerprint().contains(email)));
    }

    @Test
    void erase_auditLogAndComplianceRegisterNeverContainTheFullEmail() {
        Reservation reservation = buildReservation();
        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of(reservation));
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of());
        when(messageRepository.redactMessagesForReservations(any(), any())).thenReturn(0);
        when(reservationRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        gdprService.erase(email, method, note, actor);

        verify(auditService).record(eq(AuditAction.GDPR_DATA_ERASED), eq(actor),
                argThat(description -> !description.contains(email)), any(), any());
        verify(gdprRequestRepository).save(argThat(req -> !req.getMaskedEmail().contains(email)));
    }

    @Test
    void export_auditLogAndComplianceRegisterNeverContainTheFullEmail() {
        Reservation reservation = buildReservation();
        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of(reservation));
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of());
        when(messageRepository.findByReservationIdInOrderByCreatedAtAsc(any())).thenReturn(List.of());

        gdprService.export(email, method, note, actor);

        verify(auditService).record(eq(AuditAction.GDPR_DATA_EXPORTED), eq(actor),
                argThat(description -> !description.contains(email)), any(), any());
        verify(gdprRequestRepository).save(argThat(req -> !req.getMaskedEmail().contains(email)));
    }

    @Test
    void erase_rejectsWhenVerificationMethodMissing() {
        assertThatThrownBy(() -> gdprService.erase(email, null, note, actor))
                .isInstanceOf(BadRequestException.class);
        verify(reservationRepository, never()).findByGuestEmailIgnoreCase(any());
    }

    @Test
    void erase_rejectsWhenVerificationNoteBlank() {
        assertThatThrownBy(() -> gdprService.erase(email, method, "   ", actor))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void erase_rejectsVerificationNoteThatLooksLikeAnIdNumber() {
        assertThatThrownBy(() -> gdprService.erase(email, method, "CNP 1234567890123 confirmat", actor))
                .isInstanceOf(BadRequestException.class);
        verify(reservationRepository, never()).findByGuestEmailIgnoreCase(any());
    }

    @Test
    void erase_secondExecutionIsRejectedBecauseNothingIsLeftToErase() {
        when(reservationRepository.findByGuestEmailIgnoreCase(email))
                .thenReturn(List.of(buildReservation()))
                .thenReturn(List.of());
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of());
        when(messageRepository.redactMessagesForReservations(any(), any())).thenReturn(0);
        when(reservationRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        gdprService.erase(email, method, note, actor); // first run succeeds

        assertThatThrownBy(() -> gdprService.erase(email, method, note, actor)) // second run
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void erase_rejectsWhenNothingFound() {
        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of());
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of());

        assertThatThrownBy(() -> gdprService.erase(email, method, note, actor)).isInstanceOf(BadRequestException.class);
        verify(messageRepository, never()).redactMessagesForReservations(any(), any());
        verify(notificationRepository, never()).redactByLinkPaths(any(), any(), any());
        verify(lateCheckoutRequestRepository, never()).redactGuestNoteForReservations(any());
    }

    @Test
    void export_includesMessagesGroupedByReservationAndRecordsAudit() {
        Reservation reservation = buildReservation();
        Message guestMessage = Message.builder()
                .reservation(reservation).senderType(MessageSenderType.GUEST).body("Salut").build();

        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of(reservation));
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of());
        when(messageRepository.findByReservationIdInOrderByCreatedAtAsc(List.of(reservation.getId())))
                .thenReturn(List.of(guestMessage));

        var export = gdprService.export(email, method, note, actor);

        assertThat(export.reservations()).hasSize(1);
        assertThat(export.reservations().get(0).messages()).hasSize(1);
        assertThat(export.reservations().get(0).messages().get(0).body()).isEqualTo("Salut");
        verify(auditService).record(eq(AuditAction.GDPR_DATA_EXPORTED), eq(actor), any(), any(), any());
        // export is read-only - it must never touch messages/notifications/tokens
        verify(messageRepository, never()).redactMessagesForReservations(any(), any());
        verify(notificationRepository, never()).redactByLinkPaths(any(), any(), any());
        verify(lateCheckoutRequestRepository, never()).redactGuestNoteForReservations(any());
    }

    @Test
    void export_includesLateCheckoutRequestsGroupedByReservation() {
        Reservation reservation = buildReservation();
        LateCheckoutRequest lateCheckoutRequest = LateCheckoutRequest.builder()
                .reservation(reservation)
                .status(LateCheckoutStatus.REQUESTED)
                .guestNote("Sunt Ion, ajung cu întârziere")
                .build();

        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of(reservation));
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of());
        when(messageRepository.findByReservationIdInOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(lateCheckoutRequestRepository.findByReservationIdIn(List.of(reservation.getId())))
                .thenReturn(List.of(lateCheckoutRequest));

        var export = gdprService.export(email, method, note, actor);

        assertThat(export.reservations().get(0).lateCheckoutRequests()).hasSize(1);
        assertThat(export.reservations().get(0).lateCheckoutRequests().get(0).guestNote())
                .isEqualTo("Sunt Ion, ajung cu întârziere");
        assertThat(export.reservations().get(0).lateCheckoutRequests().get(0).status())
                .isEqualTo(LateCheckoutStatus.REQUESTED);
    }

    @Test
    void export_rejectsWhenNothingFound() {
        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of());
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of());

        assertThatThrownBy(() -> gdprService.export(email, method, note, actor)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void export_rejectsWhenVerificationMissing() {
        assertThatThrownBy(() -> gdprService.export(email, method, "", actor))
                .isInstanceOf(BadRequestException.class);
        verify(reservationRepository, never()).findByGuestEmailIgnoreCase(any());
    }

    @Test
    void export_includesAssistantChatConversationEvenWithNoReservationsOrLeads() {
        AssistantChat chat = buildAssistantChat();
        AssistantChatMessage guestMessage = AssistantChatMessage.builder()
                .chat(chat).senderType(AssistantChatSenderType.GUEST).body("Am o întrebare").build();
        AssistantChatMessage aiMessage = AssistantChatMessage.builder()
                .chat(chat).senderType(AssistantChatSenderType.AI).body("Cu ce te pot ajuta?").build();

        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of());
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of());
        when(assistantChatRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of(chat));
        when(assistantChatMessageRepository.findByChatIdInOrderByCreatedAtAsc(List.of(chat.getId())))
                .thenReturn(List.of(guestMessage, aiMessage));

        var export = gdprService.export(email, method, note, actor);

        assertThat(export.reservations()).isEmpty();
        assertThat(export.assistantChats()).hasSize(1);
        assertThat(export.assistantChats().get(0).chatId()).isEqualTo(chat.getId());
        assertThat(export.assistantChats().get(0).messages()).hasSize(2);
        assertThat(export.assistantChats().get(0).messages().get(0).body()).isEqualTo("Am o întrebare");
        verify(auditService).record(eq(AuditAction.GDPR_DATA_EXPORTED), eq(actor), any(), any(), any());
    }

    @Test
    void erase_anonymizesAssistantChatAndRedactsOnlyGuestMessages() {
        AssistantChat chat = buildAssistantChat();
        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of());
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of());
        when(assistantChatRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of(chat));
        when(assistantChatMessageRepository.redactMessagesForChats(
                List.of(chat.getId()), AssistantChatSenderType.GUEST, "[mesaj șters - solicitare GDPR]"))
                .thenReturn(2);
        when(assistantChatRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = gdprService.erase(email, method, note, actor);

        assertThat(result.assistantChatsAnonymized()).isEqualTo(1);
        assertThat(result.assistantChatMessagesRedacted()).isEqualTo(2);
        assertThat(chat.getGuestName()).isEqualTo("Șters (GDPR)");
        assertThat(chat.getGuestEmail()).isNull();

        verify(assistantChatMessageRepository).redactMessagesForChats(
                eq(List.of(chat.getId())), eq(AssistantChatSenderType.GUEST), any());
        verify(notificationRepository).redactByLinkPaths(
                eq(List.of("/dashboard/assistant-chats/" + chat.getId())), any(), any());
    }

    @Test
    void erase_allowsWhenOnlyAssistantChatMatches() {
        AssistantChat chat = buildAssistantChat();
        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of());
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of());
        when(assistantChatRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of(chat));
        when(assistantChatRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> gdprService.erase(email, method, note, actor)).doesNotThrowAnyException();
    }
}
