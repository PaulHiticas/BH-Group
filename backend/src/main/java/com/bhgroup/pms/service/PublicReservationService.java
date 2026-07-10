package com.bhgroup.pms.service;

import com.bhgroup.pms.dto.publicapi.PublicBookingRequest;
import com.bhgroup.pms.dto.publicapi.PublicBookingUpdateRequest;
import com.bhgroup.pms.dto.publicapi.PublicReservationResponse;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.dto.reservation.AvailabilityResponse;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.dto.publicapi.PublicBookingRequest;
import com.bhgroup.pms.dto.publicapi.PublicBookingUpdateRequest;
import com.bhgroup.pms.dto.publicapi.PublicReservationResponse;
import com.bhgroup.pms.dto.reservation.AvailabilityResponse;
import com.bhgroup.pms.service.mapper.PublicReservationMapper;
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
