package com.bhgroup.pms.publicapi;

import com.bhgroup.pms.mail.EmailService;
import com.bhgroup.pms.publicapi.dto.PublicBookingRequest;
import com.bhgroup.pms.publicapi.dto.PublicBookingUpdateRequest;
import com.bhgroup.pms.publicapi.dto.PublicReservationResponse;
import com.bhgroup.pms.reservation.Reservation;
import com.bhgroup.pms.reservation.ReservationService;
import com.bhgroup.pms.reservation.dto.AvailabilityResponse;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicReservationService {

    private final ReservationService reservationService;
    private final EmailService emailService;
    private final PublicReservationMapper publicReservationMapper;

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(UUID propertyId, LocalDate checkIn, LocalDate checkOut) {
        return reservationService.availability(propertyId, checkIn, checkOut);
    }

    @Transactional
    public PublicReservationResponse createBooking(PublicBookingRequest request) {
        Reservation reservation = reservationService.createGuestBooking(
                request.propertyId(), request.guestFirstName(), request.guestLastName(),
                request.guestEmail(), request.guestPhone(), request.checkInDate(), request.checkOutDate(),
                request.numberOfGuests(), request.notes());

        emailService.sendBookingConfirmationEmail(
                reservation.getGuestEmail(), reservation.getGuestFirstName(), reservation.getProperty().getName(),
                reservation.getCheckInDate().toString(), reservation.getCheckOutDate().toString(),
                reservation.getManagementToken());

        return publicReservationMapper.toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public PublicReservationResponse getByToken(String token) {
        return publicReservationMapper.toResponse(reservationService.getByManagementToken(token));
    }

    @Transactional
    public PublicReservationResponse cancelByToken(String token) {
        return publicReservationMapper.toResponse(reservationService.cancelByManagementToken(token));
    }

    @Transactional
    public PublicReservationResponse updateByToken(String token, PublicBookingUpdateRequest request) {
        Reservation reservation = reservationService.updateByManagementToken(
                token, request.checkInDate(), request.checkOutDate(), request.numberOfGuests());
        return publicReservationMapper.toResponse(reservation);
    }
}
