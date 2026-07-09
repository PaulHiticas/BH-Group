package com.bhgroup.pms.publicapi;

import com.bhgroup.pms.publicapi.dto.PublicReservationResponse;
import com.bhgroup.pms.reservation.Reservation;
import org.springframework.stereotype.Component;

@Component
public class PublicReservationMapper {

    public PublicReservationResponse toResponse(Reservation reservation) {
        return new PublicReservationResponse(
                reservation.getId(),
                reservation.getProperty().getName(),
                reservation.getProperty().getAddress().getCity(),
                reservation.getGuestFirstName(),
                reservation.getGuestLastName(),
                reservation.getGuestEmail(),
                reservation.getGuestPhone(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getNumberOfGuests(),
                reservation.getStatus(),
                reservation.getTotalAmount(),
                reservation.getCurrency(),
                reservation.getManagementToken()
        );
    }
}
