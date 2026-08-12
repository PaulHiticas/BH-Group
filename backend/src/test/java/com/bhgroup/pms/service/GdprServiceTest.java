package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.domain.AuditAction;
import com.bhgroup.pms.domain.Message;
import com.bhgroup.pms.domain.MessageSenderType;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyLead;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationSource;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.repository.MessageRepository;
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
    @Mock private MessageRepository messageRepository;
    @Mock private AuditService auditService;

    private GdprService gdprService;
    private User actor;
    private final String email = "guest@example.com";

    @BeforeEach
    void setUp() {
        gdprService = new GdprService(reservationRepository, propertyLeadRepository, messageRepository, auditService);
        actor = new User();
        actor.setId(UUID.randomUUID());
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
    void erase_anonymizesReservationAndLeadAndRedactsGuestMessages() {
        Reservation reservation = buildReservation();
        PropertyLead lead = PropertyLead.builder().fullName("Ion Popescu").email(email).phone("0722").message("Vreau info").build();
        lead.setId(UUID.randomUUID());

        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of(reservation));
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of(lead));
        when(messageRepository.redactGuestMessagesForReservations(any(), any())).thenReturn(3);
        when(reservationRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(propertyLeadRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = gdprService.erase(email, actor);

        assertThat(result.reservationsErased()).isEqualTo(1);
        assertThat(result.leadsErased()).isEqualTo(1);
        assertThat(result.messagesRedacted()).isEqualTo(3);

        assertThat(reservation.getGuestEmail()).isNull();
        assertThat(reservation.getGuestPhone()).isNull();
        assertThat(reservation.getNotes()).isNull();
        assertThat(reservation.getAccessCode()).isNull();
        assertThat(reservation.getGuestFirstName()).isEqualTo("Șters");
        // fiscally-relevant fields must survive erasure
        assertThat(reservation.getTotalAmount()).isEqualByComparingTo("500.00");
        assertThat(reservation.getCheckInDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(reservation.getProperty()).isNotNull();

        assertThat(lead.getEmail()).isNull();
        assertThat(lead.getPhone()).isNull();
        assertThat(lead.getMessage()).isNull();

        verify(auditService).record(eq(AuditAction.GDPR_DATA_ERASED), eq(actor), any(), any(), any());
    }

    @Test
    void erase_rejectsWhenNothingFound() {
        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of());
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of());

        assertThatThrownBy(() -> gdprService.erase(email, actor)).isInstanceOf(BadRequestException.class);
        verify(messageRepository, never()).redactGuestMessagesForReservations(any(), any());
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

        var export = gdprService.export(email, actor);

        assertThat(export.reservations()).hasSize(1);
        assertThat(export.reservations().get(0).messages()).hasSize(1);
        assertThat(export.reservations().get(0).messages().get(0).body()).isEqualTo("Salut");
        verify(auditService).record(eq(AuditAction.GDPR_DATA_EXPORTED), eq(actor), any(), any(), any());
    }

    @Test
    void export_rejectsWhenNothingFound() {
        when(reservationRepository.findByGuestEmailIgnoreCase(email)).thenReturn(List.of());
        when(propertyLeadRepository.findByEmailIgnoreCase(email)).thenReturn(List.of());

        assertThatThrownBy(() -> gdprService.export(email, actor)).isInstanceOf(BadRequestException.class);
    }
}
