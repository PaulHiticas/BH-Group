package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.dto.reservation.AccessCodeUpdateRequest;
import com.bhgroup.pms.dto.reservation.AvailabilityResponse;
import com.bhgroup.pms.dto.reservation.CalendarEntryResponse;
import com.bhgroup.pms.dto.reservation.ReservationCreateRequest;
import com.bhgroup.pms.dto.reservation.ReservationResponse;
import com.bhgroup.pms.dto.reservation.ReservationStatusUpdateRequest;
import com.bhgroup.pms.dto.reservation.ReservationUpdateRequest;
import com.bhgroup.pms.security.SecureTokenGenerator;
import com.bhgroup.pms.service.EmailService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationSource;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.dto.reservation.AvailabilityResponse;
import com.bhgroup.pms.dto.reservation.CalendarEntryResponse;
import com.bhgroup.pms.dto.reservation.ReservationCreateRequest;
import com.bhgroup.pms.dto.reservation.ReservationResponse;
import com.bhgroup.pms.dto.reservation.ReservationStatusUpdateRequest;
import com.bhgroup.pms.dto.reservation.ReservationUpdateRequest;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import com.bhgroup.pms.repository.ReservationSpecifications;
import com.bhgroup.pms.service.mapper.ReservationMapper;
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED_TRANSITIONS = Map.of(
            ReservationStatus.PENDING, Set.of(ReservationStatus.CONFIRMED, ReservationStatus.CANCELLED),
            ReservationStatus.CONFIRMED, Set.of(ReservationStatus.CHECKED_IN, ReservationStatus.CANCELLED,
                    ReservationStatus.NO_SHOW),
            ReservationStatus.CHECKED_IN, Set.of(ReservationStatus.CHECKED_OUT),
            ReservationStatus.CHECKED_OUT, Set.of(),
            ReservationStatus.CANCELLED, Set.of(),
            ReservationStatus.NO_SHOW, Set.of()
    );

    private final ReservationRepository reservationRepository;
    private final PropertyRepository propertyRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final ReservationMapper reservationMapper;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> list(UUID propertyId, ReservationStatus status, String search,
                                                    LocalDate from, LocalDate to, Pageable pageable) {
        Specification<Reservation> spec = ReservationSpecifications.combine(
                ReservationSpecifications.hasProperty(propertyId),
                ReservationSpecifications.hasStatus(status),
                ReservationSpecifications.search(search),
                ReservationSpecifications.checkInFrom(from),
                ReservationSpecifications.checkInTo(to)
        );

        Page<Reservation> page = reservationRepository.findAll(spec, pageable);
        return PageResponse.of(page, reservationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(UUID id) {
        return reservationMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<List<String>> exportRows(UUID propertyId, ReservationStatus status, String search,
                                          LocalDate from, LocalDate to) {
        Specification<Reservation> spec = ReservationSpecifications.combine(
                ReservationSpecifications.hasProperty(propertyId),
                ReservationSpecifications.hasStatus(status),
                ReservationSpecifications.search(search),
                ReservationSpecifications.checkInFrom(from),
                ReservationSpecifications.checkInTo(to)
        );

        return reservationRepository.findAll(spec, Sort.by("checkInDate").descending())
                .stream()
                .map(r -> List.of(
                        r.getGuestFirstName() + " " + r.getGuestLastName(),
                        r.getGuestEmail() != null ? r.getGuestEmail() : "",
                        r.getGuestPhone() != null ? r.getGuestPhone() : "",
                        r.getProperty().getName(),
                        r.getCheckInDate().toString(),
                        r.getCheckOutDate().toString(),
                        String.valueOf(r.getNumberOfGuests()),
                        r.getStatus().name(),
                        r.getSource().name(),
                        r.getTotalAmount() != null ? r.getTotalAmount().toString() : "",
                        r.getCurrency(),
                        r.getNotes() != null ? r.getNotes() : ""
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarEntryResponse> calendar(UUID propertyId, LocalDate from, LocalDate to) {
        return reservationRepository.findCalendarEntries(propertyId, from, to, ReservationStatus.NON_BLOCKING).stream()
                .map(reservationMapper::toCalendarEntry)
                .toList();
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(UUID propertyId, LocalDate checkIn, LocalDate checkOut) {
        boolean hasOverlap = !reservationRepository
                .findOverlapping(propertyId, checkIn, checkOut, null, ReservationStatus.NON_BLOCKING)
                .isEmpty();
        return new AvailabilityResponse(!hasOverlap);
    }

    @Transactional
    public ReservationResponse create(ReservationCreateRequest request) {
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        validateDates(request.checkInDate(), request.checkOutDate());
        assertNoOverlap(property.getId(), request.checkInDate(), request.checkOutDate(), null);

        Reservation reservation = Reservation.builder()
                .property(property)
                .guestFirstName(request.guestFirstName())
                .guestLastName(request.guestLastName())
                .guestEmail(request.guestEmail())
                .guestPhone(request.guestPhone())
                .checkInDate(request.checkInDate())
                .checkOutDate(request.checkOutDate())
                .numberOfGuests(request.numberOfGuests())
                .status(ReservationStatus.CONFIRMED)
                .source(request.source() != null ? request.source() : ReservationSource.DIRECT)
                .totalAmount(request.totalAmount())
                .currency(request.currency() != null && !request.currency().isBlank() ? request.currency() : "RON")
                .notes(request.notes())
                .build();

        reservation = reservationRepository.save(reservation);

        return reservationMapper.toResponse(reservation);
    }

    @Transactional
    public ReservationResponse update(UUID id, ReservationUpdateRequest request) {
        Reservation reservation = findOrThrow(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT
                || reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Cannot modify a reservation that is already checked out or cancelled");
        }

        validateDates(request.checkInDate(), request.checkOutDate());
        assertNoOverlap(reservation.getProperty().getId(), request.checkInDate(), request.checkOutDate(), id);

        reservation.setGuestFirstName(request.guestFirstName());
        reservation.setGuestLastName(request.guestLastName());
        reservation.setGuestEmail(request.guestEmail());
        reservation.setGuestPhone(request.guestPhone());
        reservation.setCheckInDate(request.checkInDate());
        reservation.setCheckOutDate(request.checkOutDate());
        reservation.setNumberOfGuests(request.numberOfGuests());
        if (request.source() != null) {
            reservation.setSource(request.source());
        }
        reservation.setTotalAmount(request.totalAmount());
        if (request.currency() != null && !request.currency().isBlank()) {
            reservation.setCurrency(request.currency());
        }
        reservation.setNotes(request.notes());

        reservation = reservationRepository.save(reservation);
        return reservationMapper.toResponse(reservation);
    }

    @Transactional
    public ReservationResponse updateStatus(UUID id, ReservationStatusUpdateRequest request) {
        Reservation reservation = findOrThrow(id);
        ReservationStatus current = reservation.getStatus();
        ReservationStatus target = request.status();

        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new BadRequestException(
                    "Cannot transition reservation from " + current + " to " + target);
        }

        reservation.setStatus(target);
        reservation = reservationRepository.save(reservation);

        return reservationMapper.toResponse(reservation);
    }

    @Transactional
    public void delete(UUID id) {
        reservationRepository.delete(findOrThrow(id));
    }

    @Transactional
    public ReservationResponse updateAccessCode(UUID id, AccessCodeUpdateRequest request) {
        Reservation reservation = findOrThrow(id);
        reservation.setAccessCode(request.accessCode() == null || request.accessCode().isBlank()
                ? null : request.accessCode().trim());
        reservation = reservationRepository.save(reservation);
        return reservationMapper.toResponse(reservation);
    }

    @Transactional
    public ReservationResponse sendCheckinInstructionsNow(UUID id) {
        Reservation reservation = findOrThrow(id);

        if (reservation.getAccessCode() == null || reservation.getAccessCode().isBlank()) {
            throw new BadRequestException("Set an access code before sending check-in instructions");
        }
        if (reservation.getGuestEmail() == null || reservation.getGuestEmail().isBlank()) {
            throw new BadRequestException("This reservation has no guest email");
        }
        if (reservation.getManagementToken() == null) {
            reservation.setManagementToken(secureTokenGenerator.generateRawToken());
        }

        sendCheckinEmail(reservation);
        reservation.setAccessCodeSentAt(Instant.now());
        reservation = reservationRepository.save(reservation);
        return reservationMapper.toResponse(reservation);
    }

    @Transactional
    public void sendPendingCheckinInstructions() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Reservation> pending = reservationRepository.findPendingCheckinInstructions(tomorrow);

        for (Reservation reservation : pending) {
            try {
                if (reservation.getManagementToken() == null) {
                    reservation.setManagementToken(secureTokenGenerator.generateRawToken());
                }
                sendCheckinEmail(reservation);
                reservation.setAccessCodeSentAt(Instant.now());
                reservationRepository.save(reservation);
            } catch (Exception ex) {
                log.error("Failed to send check-in instructions for reservation {}", reservation.getId(), ex);
            }
        }
    }

    private void sendCheckinEmail(Reservation reservation) {
        Property property = reservation.getProperty();
        String address = property.getAddress().getAddressLine() + ", " + property.getAddress().getCity();
        emailService.sendCheckinInstructionsEmail(
                reservation.getGuestEmail(),
                reservation.getGuestFirstName(),
                property.getName(),
                reservation.getCheckInDate().toString(),
                property.getCheckInTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                address,
                reservation.getAccessCode(),
                reservation.getManagementToken());
    }

    @Transactional
    public Reservation createGuestBooking(UUID propertyId, String guestFirstName, String guestLastName,
                                           String guestEmail, String guestPhone, LocalDate checkInDate,
                                           LocalDate checkOutDate, int numberOfGuests, String notes) {
        Property property = propertyRepository.findById(propertyId)
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        validateDates(checkInDate, checkOutDate);
        assertNoOverlap(propertyId, checkInDate, checkOutDate, null);

        Reservation reservation = Reservation.builder()
                .property(property)
                .guestFirstName(guestFirstName)
                .guestLastName(guestLastName)
                .guestEmail(guestEmail)
                .guestPhone(guestPhone)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .numberOfGuests(numberOfGuests)
                .status(ReservationStatus.PENDING)
                .source(ReservationSource.DIRECT)
                .totalAmount(computeTotalAmount(property, checkInDate, checkOutDate))
                .currency("RON")
                .notes(notes)
                .managementToken(secureTokenGenerator.generateRawToken())
                .build();

        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public Reservation getByManagementToken(String token) {
        return reservationRepository.findByManagementToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }

    @Transactional
    public Reservation cancelByManagementToken(String token) {
        Reservation reservation = getByManagementToken(token);
        ReservationStatus current = reservation.getStatus();

        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(ReservationStatus.CANCELLED)) {
            throw new BadRequestException("This reservation can no longer be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation updateByManagementToken(String token, LocalDate checkInDate, LocalDate checkOutDate,
                                                int numberOfGuests) {
        Reservation reservation = getByManagementToken(token);

        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT
                || reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Cannot modify a reservation that is already checked out or cancelled");
        }

        validateDates(checkInDate, checkOutDate);
        assertNoOverlap(reservation.getProperty().getId(), checkInDate, checkOutDate, reservation.getId());

        reservation.setCheckInDate(checkInDate);
        reservation.setCheckOutDate(checkOutDate);
        reservation.setNumberOfGuests(numberOfGuests);
        reservation.setTotalAmount(computeTotalAmount(reservation.getProperty(), checkInDate, checkOutDate));

        return reservationRepository.save(reservation);
    }

    private BigDecimal computeTotalAmount(Property property, LocalDate checkInDate, LocalDate checkOutDate) {
        if (property.getBasePricePerNight() == null) {
            return null;
        }
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        return property.getBasePricePerNight().multiply(BigDecimal.valueOf(nights));
    }

    private void assertNoOverlap(UUID propertyId, LocalDate checkIn, LocalDate checkOut, UUID excludeId) {
        if (!reservationRepository
                .findOverlapping(propertyId, checkIn, checkOut, excludeId, ReservationStatus.NON_BLOCKING)
                .isEmpty()) {
            throw new BadRequestException("The property is not available for the selected dates");
        }
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("Check-out date must be after check-in date");
        }
    }

    private Reservation findOrThrow(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }
}
