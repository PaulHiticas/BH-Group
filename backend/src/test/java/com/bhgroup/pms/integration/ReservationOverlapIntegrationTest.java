package com.bhgroup.pms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bhgroup.pms.domain.Address;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.PropertyType;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * The application checks availability before creating a reservation, but
 * that check-then-insert is not atomic - two concurrent requests can both
 * pass the check and both insert, double-booking the property. V8 adds a
 * Postgres GiST exclusion constraint so the database itself rejects the
 * second insert regardless of application-level races. That constraint
 * cannot be exercised by a mock or an in-memory database, only by a real
 * Postgres - this is exactly what Testcontainers is for.
 */
class ReservationOverlapIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private ReservationRepository reservationRepository;

    private Property property;

    @BeforeEach
    void setUp() {
        property = propertyRepository.save(Property.builder()
                .name("Overlap test property")
                .propertyType(PropertyType.APARTMENT)
                .status(PropertyStatus.ACTIVE)
                .address(new Address("Str. Test 1", "Cluj-Napoca", null, null, "România", null, null))
                .bedrooms(1)
                .bathrooms(1)
                .maxGuests(2)
                .build());
    }

    @Test
    void overlappingConfirmedReservationsForTheSamePropertyAreRejectedAtTheDatabaseLevel() {
        reservationRepository.saveAndFlush(reservation(
                LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 5), ReservationStatus.CONFIRMED));

        Reservation overlapping = reservation(
                LocalDate.of(2027, 6, 3), LocalDate.of(2027, 6, 8), ReservationStatus.CONFIRMED);

        assertThatThrownBy(() -> reservationRepository.saveAndFlush(overlapping))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void backToBackReservationsSharingOnlyTheChangeoverDayAreAllowed() {
        reservationRepository.saveAndFlush(reservation(
                LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 5), ReservationStatus.CONFIRMED));

        // checks in the same day the previous one checks out - not an overlap
        Reservation backToBack = reservation(
                LocalDate.of(2027, 7, 5), LocalDate.of(2027, 7, 10), ReservationStatus.CONFIRMED);

        Reservation saved = reservationRepository.saveAndFlush(backToBack);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void anOverlappingCancelledReservationDoesNotBlockANewBooking() {
        reservationRepository.saveAndFlush(reservation(
                LocalDate.of(2027, 8, 1), LocalDate.of(2027, 8, 5), ReservationStatus.CANCELLED));

        Reservation newBooking = reservation(
                LocalDate.of(2027, 8, 2), LocalDate.of(2027, 8, 6), ReservationStatus.CONFIRMED);

        Reservation saved = reservationRepository.saveAndFlush(newBooking);

        assertThat(saved.getId()).isNotNull();
    }

    private Reservation reservation(LocalDate checkIn, LocalDate checkOut, ReservationStatus status) {
        return Reservation.builder()
                .property(property)
                .guestFirstName("Test")
                .guestLastName("Guest")
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .numberOfGuests(2)
                .status(status)
                .build();
    }
}
