package com.bhgroup.pms.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bhgroup.pms.domain.Address;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.PropertyType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the actual API response contract (PublicPropertyResponse), not
 * just the UI that consumes it - the previous version silently dropped
 * addressLine entirely, so a property with showExactAddressPublicly=true
 * never actually exposed a street address despite the public page claiming
 * "the exact address is shown below".
 */
class PublicPropertyMapperTest {

    private final PublicPropertyMapper mapper = new PublicPropertyMapper(new PropertyMapper(null));

    private Property property;

    @BeforeEach
    void setUp() {
        property = Property.builder()
                .name("Test Apartment")
                .propertyType(PropertyType.APARTMENT)
                .status(PropertyStatus.ACTIVE)
                .address(new Address("Str. Exemplu 12", "Cluj-Napoca", "Cluj", "400000", "România",
                        46.770439, 23.591423))
                .bedrooms(2)
                .bathrooms(1)
                .maxGuests(4)
                .build();
        property.setId(UUID.randomUUID());
    }

    @Test
    void toResponse_exposesExactAddressAndCoordinatesWhenPropertyOptsIn() {
        property.setShowExactAddressPublicly(true);

        var response = mapper.toResponse(property, List.of());

        assertThat(response.exactLocation()).isTrue();
        assertThat(response.addressLine()).isEqualTo("Str. Exemplu 12");
        assertThat(response.latitude()).isEqualTo(46.770439);
        assertThat(response.longitude()).isEqualTo(23.591423);
    }

    @Test
    void toResponse_hidesAddressAndRoundsCoordinatesByDefault() {
        property.setShowExactAddressPublicly(false);

        var response = mapper.toResponse(property, List.of());

        assertThat(response.exactLocation()).isFalse();
        assertThat(response.addressLine()).isNull();
        // Rounded to ~2 decimal places (~1km), not the exact value.
        assertThat(response.latitude()).isEqualTo(46.77);
        assertThat(response.longitude()).isEqualTo(23.59);
    }

    @Test
    void toSummary_alsoRoundsCoordinatesWhenNotExact() {
        property.setShowExactAddressPublicly(false);

        var response = mapper.toSummary(property, List.of());

        assertThat(response.latitude()).isEqualTo(46.77);
        assertThat(response.longitude()).isEqualTo(23.59);
    }
}
