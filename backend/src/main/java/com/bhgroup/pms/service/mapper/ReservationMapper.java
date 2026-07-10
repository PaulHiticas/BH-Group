package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.dto.reservation.CalendarEntryResponse;
import com.bhgroup.pms.dto.reservation.ReservationResponse;
import org.springframework.stereotype.Component;

import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.dto.reservation.CalendarEntryResponse;
import com.bhgroup.pms.dto.reservation.ReservationResponse;
@Component
public class ReservationMapper {

    public ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getProperty().getId(),
                reservation.getProperty().getName(),
                reservation.getGuestFirstName(),
                reservation.getGuestLastName(),
                reservation.getGuestEmail(),
                reservation.getGuestPhone(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getNumberOfGuests(),
                reservation.getStatus(),
                reservation.getSource(),
                reservation.getTotalAmount(),
                reservation.getCurrency(),
                reservation.getNotes(),
                reservation.getAccessCode(),
                reservation.getAccessCodeSentAt(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }

    public CalendarEntryResponse toCalendarEntry(Reservation reservation) {
        return new CalendarEntryResponse(
                reservation.getId(),
                reservation.getGuestFullName(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getStatus(),
                reservation.getSource()
        );
    }
}
