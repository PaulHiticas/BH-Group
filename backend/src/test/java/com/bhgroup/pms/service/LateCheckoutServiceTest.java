package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.domain.LateCheckoutRequest;
import com.bhgroup.pms.domain.LateCheckoutStatus;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.repository.LateCheckoutRequestRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LateCheckoutServiceTest {

    @Mock
    private LateCheckoutRequestRepository lateCheckoutRequestRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationService reservationService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;

    private LateCheckoutService lateCheckoutService;
    private Property property;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        lateCheckoutService = new LateCheckoutService(
                lateCheckoutRequestRepository, reservationRepository, reservationService,
                notificationService, auditService);

        property = Property.builder()
                .name("Test Apartment")
                .status(PropertyStatus.ACTIVE)
                .lateCheckoutEnabled(true)
                .lateCheckoutTime(LocalTime.of(14, 0))
                .lateCheckoutFee(new BigDecimal("50.00"))
                .build();
        property.setId(UUID.randomUUID());

        reservation = Reservation.builder()
                .property(property)
                .guestFirstName("Ana")
                .guestLastName("Popescu")
                .checkInDate(LocalDate.of(2027, 6, 1))
                .checkOutDate(LocalDate.of(2027, 6, 5))
                .numberOfGuests(2)
                .status(ReservationStatus.CONFIRMED)
                .currency("RON")
                .build();
        reservation.setId(UUID.randomUUID());
    }

    @Test
    void requestByManagementToken_succeedsWhenPropertyOffersItAndNoConflict() {
        when(reservationService.getByManagementToken("tok")).thenReturn(reservation);
        when(lateCheckoutRequestRepository.findByReservationId(reservation.getId())).thenReturn(Optional.empty());
        when(reservationRepository.existsByPropertyIdAndCheckInDateAndStatusNotIn(
                eq(property.getId()), eq(reservation.getCheckOutDate()), any())).thenReturn(false);
        when(lateCheckoutRequestRepository.saveAndFlush(any(LateCheckoutRequest.class)))
                .thenAnswer(invocation -> {
                    LateCheckoutRequest r = invocation.getArgument(0);
                    r.setId(UUID.randomUUID());
                    return r;
                });

        var response = lateCheckoutService.requestByManagementToken("tok", "Zbor seara");

        assertThat(response.status()).isEqualTo(LateCheckoutStatus.REQUESTED);
        assertThat(response.requestedCheckoutTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.fee()).isEqualByComparingTo("50.00");
    }

    @Test
    void request_rejectsWhenPropertyDoesNotOfferLateCheckout() {
        property.setLateCheckoutEnabled(false);
        when(reservationService.getByManagementToken("tok")).thenReturn(reservation);

        assertThatThrownBy(() -> lateCheckoutService.requestByManagementToken("tok", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("nu este disponibil");
    }

    @Test
    void request_rejectsWhenAReservationAlreadyHasARequest() {
        when(reservationService.getByManagementToken("tok")).thenReturn(reservation);
        when(lateCheckoutRequestRepository.findByReservationId(reservation.getId()))
                .thenReturn(Optional.of(new LateCheckoutRequest()));

        assertThatThrownBy(() -> lateCheckoutService.requestByManagementToken("tok", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Există deja");
    }

    @Test
    void request_rejectsWhenNextGuestChecksInTheSameDayNoCleaningBuffer() {
        when(reservationService.getByManagementToken("tok")).thenReturn(reservation);
        when(lateCheckoutRequestRepository.findByReservationId(reservation.getId())).thenReturn(Optional.empty());
        when(reservationRepository.existsByPropertyIdAndCheckInDateAndStatusNotIn(
                eq(property.getId()), eq(reservation.getCheckOutDate()), any())).thenReturn(true);

        assertThatThrownBy(() -> lateCheckoutService.requestByManagementToken("tok", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("fără timp pentru curățenie");
    }

    @Test
    void request_rejectsWhenReservationIsNotConfirmedOrCheckedIn() {
        reservation.setStatus(ReservationStatus.PENDING);
        when(reservationService.getByManagementToken("tok")).thenReturn(reservation);

        assertThatThrownBy(() -> lateCheckoutService.requestByManagementToken("tok", null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void approve_rejectsWhenRequestIsNotInRequestedStatus() {
        LateCheckoutRequest request = existingRequest(LateCheckoutStatus.APPROVED);
        when(lateCheckoutRequestRepository.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> lateCheckoutService.approve(request.getId(), mockUser()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void approve_reChecksTheCleaningBufferAndRejectsIfAConflictAppearedSinceTheRequest() {
        LateCheckoutRequest request = existingRequest(LateCheckoutStatus.REQUESTED);
        when(lateCheckoutRequestRepository.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        // A conflicting reservation now exists, even though it didn't when the guest first asked.
        when(reservationRepository.existsByPropertyIdAndCheckInDateAndStatusNotIn(
                eq(property.getId()), eq(reservation.getCheckOutDate()), any())).thenReturn(true);

        assertThatThrownBy(() -> lateCheckoutService.approve(request.getId(), mockUser()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("fără timp pentru curățenie");
    }

    @Test
    void approve_succeedsWhenTheCleaningBufferIsStillAvailable() {
        LateCheckoutRequest request = existingRequest(LateCheckoutStatus.REQUESTED);
        when(lateCheckoutRequestRepository.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(reservationRepository.existsByPropertyIdAndCheckInDateAndStatusNotIn(
                eq(property.getId()), eq(reservation.getCheckOutDate()), any())).thenReturn(false);
        when(lateCheckoutRequestRepository.save(any(LateCheckoutRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = lateCheckoutService.approve(request.getId(), mockUser());

        assertThat(response.status()).isEqualTo(LateCheckoutStatus.APPROVED);
    }

    @Test
    void markPaid_rejectsUnlessRequestIsApproved() {
        LateCheckoutRequest request = existingRequest(LateCheckoutStatus.REQUESTED);
        when(lateCheckoutRequestRepository.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> lateCheckoutService.markPaid(request.getId(), mockUser()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("aprobate");
    }

    @Test
    void markPaid_succeedsWhenRequestIsApproved() {
        LateCheckoutRequest request = existingRequest(LateCheckoutStatus.APPROVED);
        when(lateCheckoutRequestRepository.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(lateCheckoutRequestRepository.save(any(LateCheckoutRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = lateCheckoutService.markPaid(request.getId(), mockUser());

        assertThat(response.status()).isEqualTo(LateCheckoutStatus.PAID);
    }

    private LateCheckoutRequest existingRequest(LateCheckoutStatus status) {
        LateCheckoutRequest request = LateCheckoutRequest.builder()
                .reservation(reservation)
                .requestedCheckoutTime(LocalTime.of(14, 0))
                .fee(new BigDecimal("50.00"))
                .currency("RON")
                .status(status)
                .build();
        request.setId(UUID.randomUUID());
        return request;
    }

    private User mockUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }
}
