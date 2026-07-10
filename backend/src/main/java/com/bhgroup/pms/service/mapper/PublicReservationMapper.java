package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.dto.publicapi.PublicReservationResponse;
import com.bhgroup.pms.domain.Reservation;
import org.springframework.stereotype.Component;

import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.dto.publicapi.PublicReservationResponse;
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
