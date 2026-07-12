package com.bhgroup.pms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every hour, pulls every registered Airbnb/Booking.com .ics feed and syncs
 * its bookings into the calendar as blocking reservations. See
 * {@link IcalImportService#syncAllFeeds()}.
 */
@Component
@RequiredArgsConstructor
public class IcalSyncScheduler {

    private final IcalImportService icalImportService;

    @Scheduled(fixedRate = 3_600_000)
    public void syncAllFeeds() {
        icalImportService.syncAllFeeds();
    }
}
