package com.bhgroup.pms.reservation;

import com.bhgroup.pms.reservation.dto.CalendarEntryResponse;
import com.bhgroup.pms.reservation.dto.ReservationResponse;
import org.springframework.stereotype.Component;

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
                reservation.getStatus()
        );
    }
}
