package com.bhgroup.pms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every hour, emails check-in instructions (address, time, access code) to
 * guests checking in tomorrow, for reservations where staff has already set
 * an access code. Manual per-reservation sending is also available via
 * {@link ReservationService#sendCheckinInstructionsNow}.
 */
@Component
@RequiredArgsConstructor
public class CheckinReminderScheduler {

    private final ReservationService reservationService;

    @Scheduled(cron = "0 0 * * * *")
    public void sendPendingCheckinInstructions() {
        reservationService.sendPendingCheckinInstructions();
    }
}
