package com.bhgroup.pms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.bhgroup.pms.domain.Address;
import com.bhgroup.pms.domain.GdprVerificationMethod;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.PropertyType;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import com.bhgroup.pms.service.AuditService;
import com.bhgroup.pms.service.GdprService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * A mocked-repository unit test can prove the anonymization logic is
 * correct, but it can't prove @Transactional actually rolls back a
 * half-applied change in a real database - that requires a real
 * Postgres transaction to genuinely commit or abort. This forces
 * {@link GdprService#erase} to fail after the reservation has already
 * been mutated and saved in-memory (but before the method returns), then
 * re-reads the reservation with a fresh query to confirm nothing was
 * actually persisted.
 */
class GdprEraseRollbackIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private GdprService gdprService;
    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @MockBean
    private AuditService auditService;

    private static final String GUEST_EMAIL = "rollback-test@example.com";

    @Test
    void erase_rollsBackReservationAnonymizationWhenAWriteFailsPartwayThrough() {
        Property property = propertyRepository.save(Property.builder()
                .name("Rollback test property")
                .propertyType(PropertyType.APARTMENT)
                .status(PropertyStatus.ACTIVE)
                .address(new Address("Str. Test 1", "Cluj-Napoca", null, null, "România", null, null))
                .bedrooms(1)
                .bathrooms(1)
                .maxGuests(2)
                .build());

        Reservation reservation = reservationRepository.saveAndFlush(Reservation.builder()
                .property(property)
                .guestFirstName("Ion")
                .guestLastName("Popescu")
                .guestEmail(GUEST_EMAIL)
                .guestPhone("0722000000")
                .checkInDate(LocalDate.of(2027, 9, 1))
                .checkOutDate(LocalDate.of(2027, 9, 5))
                .numberOfGuests(2)
                .status(ReservationStatus.CONFIRMED)
                .build());
        UUID reservationId = reservation.getId();

        // AuditService runs after the reservation has already been anonymized and saveAll'd
        // within the same @Transactional method - simulates a failure partway through.
        doThrow(new RuntimeException("simulated failure after the reservation write"))
                .when(auditService).record(any(), any(), any(), any(), any());

        User actor = new User();
        actor.setId(UUID.randomUUID());

        assertThatThrownBy(() -> gdprService.erase(
                GUEST_EMAIL, GdprVerificationMethod.RESERVATION_DETAILS, "Verificat telefonic", actor))
                .isInstanceOf(RuntimeException.class);

        Reservation reread = reservationRepository.findById(reservationId).orElseThrow();
        assertThat(reread.getGuestEmail()).isEqualTo(GUEST_EMAIL);
        assertThat(reread.getGuestFirstName()).isEqualTo("Ion");
        assertThat(reread.getGuestLastName()).isEqualTo("Popescu");
        assertThat(reread.getGuestPhone()).isEqualTo("0722000000");
    }
}
