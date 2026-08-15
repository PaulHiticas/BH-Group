package com.bhgroup.pms.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bhgroup.pms.domain.Address;
import com.bhgroup.pms.domain.LocalEvent;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.PropertyType;
import com.bhgroup.pms.repository.LocalEventRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The city-scoping and normalization the dynamic pricing engine relies on
 * ({@code lower(trim(city))} matching, plus the property-specific override)
 * lives in {@link LocalEventRepository}'s JPQL query, not in Java - so it
 * needs a real database to exercise, unlike {@code PricingServiceTest}
 * (which mocks this repository and can't verify the query itself).
 */
class LocalEventRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private LocalEventRepository localEventRepository;

    private Property clujProperty;
    private Property brasovProperty;

    @BeforeEach
    void setUp() {
        // The Testcontainers Postgres instance is shared (static) across the whole suite with no
        // per-test rollback, so events from earlier test methods must be cleared explicitly.
        localEventRepository.deleteAll();
        clujProperty = propertyRepository.save(property("Cluj-Napoca"));
        brasovProperty = propertyRepository.save(property("Brasov"));
    }

    @Test
    void eventScopedToCity_appliesToPropertyInThatCity() {
        localEventRepository.save(cityEvent("Cluj-Napoca", LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 5)));

        List<LocalEvent> relevant = localEventRepository.findRelevantForPricing(
                clujProperty.getId(), "cluj-napoca", LocalDate.of(2027, 9, 2), LocalDate.of(2027, 9, 2));

        assertThat(relevant).hasSize(1);
    }

    @Test
    void eventScopedToCity_doesNotApplyToPropertyInADifferentCity() {
        localEventRepository.save(cityEvent("Cluj-Napoca", LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 5)));

        List<LocalEvent> relevant = localEventRepository.findRelevantForPricing(
                brasovProperty.getId(), "brasov", LocalDate.of(2027, 9, 2), LocalDate.of(2027, 9, 2));

        assertThat(relevant).isEmpty();
    }

    @Test
    void cityMatching_isCaseInsensitiveAndTrimmed() {
        localEventRepository.save(cityEvent("  CLUJ-Napoca  ", LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 5)));

        List<LocalEvent> relevant = localEventRepository.findRelevantForPricing(
                clujProperty.getId(), "cluj-napoca", LocalDate.of(2027, 9, 2), LocalDate.of(2027, 9, 2));

        assertThat(relevant).hasSize(1);
    }

    @Test
    void propertyOverride_appliesRegardlessOfCity() {
        LocalEvent override = LocalEvent.builder()
                .property(brasovProperty)
                .label("Private festival")
                .startDate(LocalDate.of(2027, 9, 1))
                .endDate(LocalDate.of(2027, 9, 5))
                .priceMultiplier(new BigDecimal("2.00"))
                .build();
        localEventRepository.save(override);

        List<LocalEvent> relevantForBrasov = localEventRepository.findRelevantForPricing(
                brasovProperty.getId(), "brasov", LocalDate.of(2027, 9, 2), LocalDate.of(2027, 9, 2));
        List<LocalEvent> relevantForCluj = localEventRepository.findRelevantForPricing(
                clujProperty.getId(), "cluj-napoca", LocalDate.of(2027, 9, 2), LocalDate.of(2027, 9, 2));

        assertThat(relevantForBrasov).hasSize(1);
        assertThat(relevantForCluj).isEmpty();
    }

    @Test
    void eventOutsideTheQueriedDateRange_isNotReturned() {
        localEventRepository.save(cityEvent("Cluj-Napoca", LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 5)));

        List<LocalEvent> relevant = localEventRepository.findRelevantForPricing(
                clujProperty.getId(), "cluj-napoca", LocalDate.of(2027, 10, 1), LocalDate.of(2027, 10, 2));

        assertThat(relevant).isEmpty();
    }

    private Property property(String city) {
        return Property.builder()
                .name("Property in " + city)
                .propertyType(PropertyType.APARTMENT)
                .status(PropertyStatus.ACTIVE)
                .address(new Address("Str. Test 1", city, null, null, "România", null, null))
                .bedrooms(1)
                .bathrooms(1)
                .maxGuests(2)
                .build();
    }

    private LocalEvent cityEvent(String city, LocalDate start, LocalDate end) {
        return LocalEvent.builder()
                .city(city)
                .label("Festival")
                .startDate(start)
                .endDate(end)
                .priceMultiplier(new BigDecimal("1.50"))
                .build();
    }
}
