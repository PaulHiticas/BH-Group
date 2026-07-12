package com.bhgroup.pms.service;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.domain.IcalImportFeed;
import com.bhgroup.pms.domain.IcalSyncStatus;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.dto.ical.IcalImportFeedCreateRequest;
import com.bhgroup.pms.dto.ical.IcalImportFeedResponse;
import com.bhgroup.pms.repository.IcalImportFeedRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import com.bhgroup.pms.service.mapper.IcalImportFeedMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pulls external Airbnb/Booking.com .ics feeds and mirrors each VEVENT as a
 * blocking reservation (no price, source-tagged), keyed by a
 * feed-namespaced UID so re-syncs update or remove cleanly instead of
 * duplicating. A date range that's already taken by a real PMS booking is
 * skipped and surfaced as a conflict rather than silently overwritten.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IcalImportService {

    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(20);

    private final IcalImportFeedRepository icalImportFeedRepository;
    private final PropertyRepository propertyRepository;
    private final ReservationRepository reservationRepository;
    private final IcalImportFeedMapper icalImportFeedMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(FETCH_TIMEOUT).build();

    @Transactional(readOnly = true)
    public List<IcalImportFeedResponse> listFeeds(UUID propertyId) {
        return icalImportFeedRepository.findByPropertyIdOrderByCreatedAtAsc(propertyId).stream()
                .map(icalImportFeedMapper::toResponse)
                .toList();
    }

    @Transactional
    public IcalImportFeedResponse addFeed(UUID propertyId, IcalImportFeedCreateRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        IcalImportFeed feed = IcalImportFeed.builder()
                .property(property)
                .source(request.source())
                .feedUrl(request.feedUrl())
                .build();

        return icalImportFeedMapper.toResponse(icalImportFeedRepository.save(feed));
    }

    @Transactional
    public void deleteFeed(UUID propertyId, UUID feedId) {
        IcalImportFeed feed = findFeedOrThrow(propertyId, feedId);
        List<Reservation> imported = reservationRepository.findByPropertyIdAndExternalUidIsNotNull(propertyId);
        String prefix = feedId + ":";
        imported.stream()
                .filter(r -> r.getExternalUid().startsWith(prefix))
                .forEach(reservationRepository::delete);
        icalImportFeedRepository.delete(feed);
    }

    @Transactional
    public IcalImportFeedResponse syncFeed(UUID propertyId, UUID feedId) {
        return syncFeed(findFeedOrThrow(propertyId, feedId));
    }

    /** Invoked by {@link IcalSyncScheduler} for every registered feed. */
    @Transactional
    public void syncAllFeeds() {
        for (IcalImportFeed feed : icalImportFeedRepository.findAll()) {
            try {
                syncFeed(feed);
            } catch (Exception ex) {
                log.error("Unexpected error syncing iCal feed {}", feed.getId(), ex);
            }
        }
    }

    private IcalImportFeedResponse syncFeed(IcalImportFeed feed) {
        String prefix = feed.getId() + ":";
        try {
            ICalendar calendar = fetchAndParse(feed.getFeedUrl());
            Set<String> seenUids = new HashSet<>();
            int conflicts = 0;

            for (VEvent event : calendar.getEvents()) {
                if (event.getUid() == null || event.getDateStart() == null || event.getDateEnd() == null) {
                    continue;
                }
                String uid = prefix + event.getUid().getValue();
                LocalDate start = toLocalDate(event.getDateStart().getValue());
                LocalDate end = toLocalDate(event.getDateEnd().getValue());
                if (!end.isAfter(start)) {
                    continue;
                }
                seenUids.add(uid);

                if (!upsertBlock(feed, uid, start, end)) {
                    conflicts++;
                }
            }

            reservationRepository.findByPropertyIdAndExternalUidIsNotNull(feed.getProperty().getId()).stream()
                    .filter(r -> r.getExternalUid().startsWith(prefix) && !seenUids.contains(r.getExternalUid()))
                    .forEach(reservationRepository::delete);

            feed.setLastSyncedAt(Instant.now());
            feed.setLastSyncStatus(IcalSyncStatus.SUCCESS);
            feed.setLastSyncError(conflicts > 0
                    ? conflicts + " eveniment(e) ignorate din cauza unui conflict cu o rezervare existentă"
                    : null);
        } catch (Exception ex) {
            feed.setLastSyncedAt(Instant.now());
            feed.setLastSyncStatus(IcalSyncStatus.FAILED);
            feed.setLastSyncError(truncate(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
            log.warn("iCal sync failed for feed {}", feed.getId(), ex);
        }

        return icalImportFeedMapper.toResponse(icalImportFeedRepository.save(feed));
    }

    /** Returns false (and skips the write) if the range conflicts with a real, non-imported booking. */
    private boolean upsertBlock(IcalImportFeed feed, String uid, LocalDate start, LocalDate end) {
        Optional<Reservation> existing = reservationRepository.findByExternalUid(uid);
        UUID excludeId = existing.map(Reservation::getId).orElse(null);

        boolean conflicts = !reservationRepository
                .findOverlapping(feed.getProperty().getId(), start, end, excludeId, ReservationStatus.NON_BLOCKING)
                .isEmpty();
        if (conflicts) {
            return false;
        }

        Reservation reservation = existing.orElseGet(() -> Reservation.builder()
                .property(feed.getProperty())
                .guestFirstName(sourceLabel(feed))
                .guestLastName("(sincronizat)")
                .numberOfGuests(1)
                .status(ReservationStatus.CONFIRMED)
                .source(feed.getSource())
                .currency("RON")
                .externalUid(uid)
                .build());

        reservation.setCheckInDate(start);
        reservation.setCheckOutDate(end);
        reservationRepository.save(reservation);
        return true;
    }

    private String sourceLabel(IcalImportFeed feed) {
        return switch (feed.getSource()) {
            case AIRBNB -> "Airbnb";
            case BOOKING_COM -> "Booking.com";
            default -> "Extern";
        };
    }

    private ICalendar fetchAndParse(String feedUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(feedUrl))
                .timeout(FETCH_TIMEOUT)
                .header("Accept", "text/calendar")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Feed returned HTTP " + response.statusCode());
        }
        ICalendar calendar = Biweekly.parse(response.body()).first();
        if (calendar == null) {
            throw new IllegalStateException("Feed did not contain a valid calendar");
        }
        return calendar;
    }

    private LocalDate toLocalDate(java.util.Date date) {
        return date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private String truncate(String message) {
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private IcalImportFeed findFeedOrThrow(UUID propertyId, UUID feedId) {
        return icalImportFeedRepository.findById(feedId)
                .filter(f -> f.getProperty().getId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("iCal feed not found"));
    }
}
