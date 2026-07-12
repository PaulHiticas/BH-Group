package com.bhgroup.pms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Once a day, sends a one-time in-app reminder for property documents
 * (contracts, insurance, utility bills) that are expired or about to
 * expire. See {@link PropertyService#notifyExpiringDocuments()}.
 */
@Component
@RequiredArgsConstructor
public class DocumentExpiryScheduler {

    private final PropertyService propertyService;

    @Scheduled(cron = "0 0 7 * * *")
    public void notifyExpiringDocuments() {
        propertyService.notifyExpiringDocuments();
    }
}
