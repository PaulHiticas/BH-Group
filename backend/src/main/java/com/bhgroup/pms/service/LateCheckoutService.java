package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.domain.AuditAction;
import com.bhgroup.pms.domain.LateCheckoutRequest;
import com.bhgroup.pms.domain.LateCheckoutStatus;
import com.bhgroup.pms.domain.NotificationType;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.latecheckout.LateCheckoutRequestResponse;
import com.bhgroup.pms.repository.LateCheckoutRequestRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Late checkout is request-only: the guest asks, staff approves/rejects,
 * and once approved someone marks it paid after collecting payment
 * out-of-band. There is no real payment integration yet - PAID is a manual
 * staff action, not triggered by a payment gateway callback.
 */
@Service
@RequiredArgsConstructor
public class LateCheckoutService {

    private final LateCheckoutRequestRepository lateCheckoutRequestRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public LateCheckoutRequestResponse requestByManagementToken(String token, String guestNote) {
        Reservation reservation = reservationService.getByManagementToken(token);
        return toResponse(createRequest(reservation, guestNote));
    }

    @Transactional
    public LateCheckoutRequestResponse requestForReservation(UUID reservationId, String guestNote) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        return toResponse(createRequest(reservation, guestNote));
    }

    private LateCheckoutRequest createRequest(Reservation reservation, String guestNote) {
        if (reservation.getStatus() != ReservationStatus.CONFIRMED
                && reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new BadRequestException(
                    "Check-out târziu poate fi cerut doar pentru o rezervare confirmată sau în desfășurare");
        }

        if (lateCheckoutRequestRepository.findByReservationId(reservation.getId()).isPresent()) {
            throw new BadRequestException("Există deja o cerere de check-out târziu pentru această rezervare");
        }

        Property property = reservation.getProperty();
        if (!property.isLateCheckoutEnabled() || property.getLateCheckoutTime() == null) {
            throw new BadRequestException("Check-out târziu nu este disponibil pentru această proprietate");
        }

        boolean nextGuestSameDay = reservationRepository.existsByPropertyIdAndCheckInDateAndStatusNotIn(
                property.getId(), reservation.getCheckOutDate(), ReservationStatus.NON_BLOCKING);
        if (nextGuestSameDay) {
            throw new BadRequestException(
                    "Check-out târziu nu este disponibil: următorul oaspete face check-in chiar în ziua "
                            + "check-out-ului, fără timp pentru curățenie");
        }

        LateCheckoutRequest request = LateCheckoutRequest.builder()
                .reservation(reservation)
                .requestedCheckoutTime(property.getLateCheckoutTime())
                .fee(property.getLateCheckoutFee())
                .currency("RON")
                .status(LateCheckoutStatus.REQUESTED)
                .guestNote(guestNote)
                .build();
        request = lateCheckoutRequestRepository.save(request);

        notificationService.notifyAdmins(NotificationType.LATE_CHECKOUT_REQUEST,
                "Cerere check-out târziu",
                reservation.getGuestFirstName() + " " + reservation.getGuestLastName() + " — "
                        + property.getName(),
                "/dashboard/reservations/" + reservation.getId());

        auditService.record(AuditAction.LATE_CHECKOUT_REQUESTED, null,
                "Late checkout requested for reservation " + reservation.getId(), null, null);

        return request;
    }

    @Transactional(readOnly = true)
    public Optional<LateCheckoutRequestResponse> getByReservationId(UUID reservationId) {
        return lateCheckoutRequestRepository.findByReservationId(reservationId).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<LateCheckoutRequestResponse> getByManagementToken(String token) {
        Reservation reservation = reservationService.getByManagementToken(token);
        return lateCheckoutRequestRepository.findByReservationId(reservation.getId()).map(this::toResponse);
    }

    @Transactional
    public LateCheckoutRequestResponse approve(UUID requestId, User actor) {
        LateCheckoutRequest request = findOrThrow(requestId);
        if (request.getStatus() != LateCheckoutStatus.REQUESTED) {
            throw new BadRequestException("Doar cererile în așteptare pot fi aprobate");
        }
        request.setStatus(LateCheckoutStatus.APPROVED);
        request.setDecidedBy(actor);
        request.setDecidedAt(Instant.now());
        request = lateCheckoutRequestRepository.save(request);

        auditService.record(AuditAction.LATE_CHECKOUT_APPROVED, actor,
                "Late checkout request " + request.getId() + " approved", null, null);

        return toResponse(request);
    }

    @Transactional
    public LateCheckoutRequestResponse reject(UUID requestId, User actor) {
        LateCheckoutRequest request = findOrThrow(requestId);
        if (request.getStatus() != LateCheckoutStatus.REQUESTED) {
            throw new BadRequestException("Doar cererile în așteptare pot fi respinse");
        }
        request.setStatus(LateCheckoutStatus.REJECTED);
        request.setDecidedBy(actor);
        request.setDecidedAt(Instant.now());
        request = lateCheckoutRequestRepository.save(request);

        auditService.record(AuditAction.LATE_CHECKOUT_REJECTED, actor,
                "Late checkout request " + request.getId() + " rejected", null, null);

        return toResponse(request);
    }

    @Transactional
    public LateCheckoutRequestResponse markPaid(UUID requestId, User actor) {
        LateCheckoutRequest request = findOrThrow(requestId);
        if (request.getStatus() != LateCheckoutStatus.APPROVED) {
            throw new BadRequestException("Doar cererile aprobate pot fi marcate ca plătite");
        }
        request.setStatus(LateCheckoutStatus.PAID);
        request.setPaidAt(Instant.now());
        request = lateCheckoutRequestRepository.save(request);

        auditService.record(AuditAction.LATE_CHECKOUT_MARKED_PAID, actor,
                "Late checkout request " + request.getId() + " marked paid", null, null);

        return toResponse(request);
    }

    private LateCheckoutRequest findOrThrow(UUID id) {
        return lateCheckoutRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Late checkout request not found"));
    }

    private LateCheckoutRequestResponse toResponse(LateCheckoutRequest request) {
        return new LateCheckoutRequestResponse(
                request.getId(),
                request.getReservation().getId(),
                request.getRequestedCheckoutTime(),
                request.getFee(),
                request.getCurrency(),
                request.getStatus(),
                request.getGuestNote(),
                request.getCreatedAt(),
                request.getDecidedAt(),
                request.getPaidAt());
    }
}
