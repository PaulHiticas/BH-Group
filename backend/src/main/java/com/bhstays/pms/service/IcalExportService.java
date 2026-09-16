package com.bhstays.pms.service;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.DateEnd;
import biweekly.property.DateStart;
import com.bhstays.pms.common.exception.ResourceNotFoundException;
import com.bhstays.pms.domain.Property;
import com.bhstays.pms.domain.Reservation;
import com.bhstays.pms.domain.ReservationStatus;
import com.bhstays.pms.repository.PropertyRepository;
import com.bhstays.pms.repository.ReservationRepository;
import com.bhstays.pms.security.SecureTokenGenerator;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates the .ics "busy dates" feed for a property, so Airbnb/Booking.com
 * can be told to block dates already reserved through BH Stays. Deliberately
 * excludes guest names/contact details — OTAs and anyone with the feed URL
 * only need to know a date range is unavailable, not who's staying.
 */
@Service
@RequiredArgsConstructor
public class IcalExportService {

    private final PropertyRepository propertyRepository;
    private final ReservationRepository reservationRepository;
    private final SecureTokenGenerator secureTokenGenerator;

    @Transactional
    public String getOrCreateExportToken(UUID propertyId) {
        Property property = findPropertyOrThrow(propertyId);
        if (property.getIcalExportToken() == null) {
            property.setIcalExportToken(secureTokenGenerator.generateRawToken());
            propertyRepository.save(property);
        }
        return property.getIcalExportToken();
    }

    @Transactional
    public String regenerateExportToken(UUID propertyId) {
        Property property = findPropertyOrThrow(propertyId);
        property.setIcalExportToken(secureTokenGenerator.generateRawToken());
        propertyRepository.save(property);
        return property.getIcalExportToken();
    }

    @Transactional(readOnly = true)
    public String generateFeed(UUID propertyId, String token) {
        Property property = findPropertyOrThrow(propertyId);
        if (property.getIcalExportToken() == null || !property.getIcalExportToken().equals(token)) {
            throw new ResourceNotFoundException("Property not found");
        }

        ICalendar calendar = new ICalendar();
        calendar.setProductId("-//BH Stays//Property Calendar//EN");

        for (Reservation reservation : reservationRepository
                .findActiveForExport(propertyId, ReservationStatus.NON_BLOCKING)) {
            VEvent event = new VEvent();
            event.setUid(reservation.getId().toString());
            event.setDateStart(new DateStart(toDate(reservation.getCheckInDate()), false));
            event.setDateEnd(new DateEnd(toDate(reservation.getCheckOutDate()), false));
            event.setSummary("Rezervat - BH Stays");
            calendar.addEvent(event);
        }

        return Biweekly.write(calendar).go();
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private Property findPropertyOrThrow(UUID propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
    }
}
