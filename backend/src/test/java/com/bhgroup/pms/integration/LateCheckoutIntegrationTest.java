package com.bhgroup.pms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.domain.Address;
import com.bhgroup.pms.domain.LateCheckoutRequest;
import com.bhgroup.pms.domain.LateCheckoutStatus;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.PropertyType;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.domain.UserStatus;
import com.bhgroup.pms.repository.LateCheckoutRequestRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.LateCheckoutService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Exercises the real Postgres constraints and the actual re-check-at-
 * approval logic - a mocked-repository unit test can assert the code calls
 * the right method, but only a real database can prove the unique
 * constraint actually rejects a second row, and only the real service
 * wiring proves approve() re-evaluates the cleaning buffer instead of
 * trusting the state from when the request was made.
 */
class LateCheckoutIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private LateCheckoutRequestRepository lateCheckoutRequestRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LateCheckoutService lateCheckoutService;

    private Property property;
    private Reservation reservation;
    private User superAdmin;

    @BeforeEach
    void setUp() {
        property = propertyRepository.save(Property.builder()
                .name("Late checkout test property")
                .propertyType(PropertyType.APARTMENT)
                .status(PropertyStatus.ACTIVE)
                .address(new Address("Str. Test 3", "Cluj-Napoca", null, null, "România", null, null))
                .bedrooms(1)
                .bathrooms(1)
                .maxGuests(2)
                .lateCheckoutEnabled(true)
                .lateCheckoutTime(LocalTime.of(14, 0))
                .lateCheckoutFee(new BigDecimal("50.00"))
                .build());

        reservation = reservationRepository.saveAndFlush(Reservation.builder()
                .property(property)
                .guestFirstName("Ana")
                .guestLastName("Popescu")
                .checkInDate(LocalDate.of(2027, 9, 1))
                .checkOutDate(LocalDate.of(2027, 9, 5))
                .numberOfGuests(2)
                .status(ReservationStatus.CONFIRMED)
                .currency("RON")
                .build());

        superAdmin = userRepository.save(User.builder()
                .email("admin-" + System.nanoTime() + "@bhgroup.io")
                .passwordHash("irrelevant-for-this-test")
                .firstName("Admin")
                .lastName("Test")
                .role(Role.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
    }

    @Test
    void aSecondLateCheckoutRequestForTheSameReservationIsRejectedAtTheDatabaseLevel() {
        lateCheckoutRequestRepository.saveAndFlush(LateCheckoutRequest.builder()
                .reservation(reservation)
                .requestedCheckoutTime(LocalTime.of(14, 0))
                .fee(new BigDecimal("50.00"))
                .currency("RON")
                .status(LateCheckoutStatus.REQUESTED)
                .build());

        LateCheckoutRequest duplicate = LateCheckoutRequest.builder()
                .reservation(reservation)
                .requestedCheckoutTime(LocalTime.of(14, 0))
                .fee(new BigDecimal("50.00"))
                .currency("RON")
                .status(LateCheckoutStatus.REQUESTED)
                .build();

        assertThatThrownBy(() -> lateCheckoutRequestRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void approvalIsBlockedWhenAConflictingReservationAppearsAfterTheRequestWasMade() {
        var response = lateCheckoutService.requestForReservation(reservation.getId(), null);
        assertThat(response.status()).isEqualTo(LateCheckoutStatus.REQUESTED);

        // A new booking checks in exactly the day our reservation checks out -
        // created after the late-checkout request, simulating the race the
        // review flagged (nothing stopped a new reservation from appearing
        // between request and approval).
        reservationRepository.saveAndFlush(Reservation.builder()
                .property(property)
                .guestFirstName("Ion")
                .guestLastName("Vasile")
                .checkInDate(LocalDate.of(2027, 9, 5))
                .checkOutDate(LocalDate.of(2027, 9, 8))
                .numberOfGuests(1)
                .status(ReservationStatus.CONFIRMED)
                .currency("RON")
                .build());

        assertThatThrownBy(() -> lateCheckoutService.approve(response.id(), superAdmin))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("fără timp pentru curățenie");

        // The request must still be sitting in REQUESTED, not silently approved.
        var reloaded = lateCheckoutRequestRepository.findById(response.id()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(LateCheckoutStatus.REQUESTED);
    }

    @Test
    void approvalSucceedsWhenNoConflictingReservationExists() {
        var response = lateCheckoutService.requestForReservation(reservation.getId(), null);

        var approved = lateCheckoutService.approve(response.id(), superAdmin);

        assertThat(approved.status()).isEqualTo(LateCheckoutStatus.APPROVED);
    }
}
