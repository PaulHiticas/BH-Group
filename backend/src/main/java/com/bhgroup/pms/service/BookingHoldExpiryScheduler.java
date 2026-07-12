package com.bhgroup.pms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every minute, cancels PENDING guest bookings whose payment hold has
 * expired, freeing the calendar for other guests. See
 * {@link ReservationService#expireStaleHolds()}.
 */
@Component
@RequiredArgsConstructor
public class BookingHoldExpiryScheduler {

    private final ReservationService reservationService;

    @Scheduled(fixedRate = 60_000)
    public void expireStaleHolds() {
        reservationService.expireStaleHolds();
    }
}
