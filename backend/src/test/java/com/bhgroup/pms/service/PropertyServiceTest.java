package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.domain.AuditAction;
import com.bhgroup.pms.domain.IntegrationMode;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyType;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.property.PropertyCreateRequest;
import com.bhgroup.pms.dto.property.PropertyResponse;
import com.bhgroup.pms.repository.PropertyDocumentRepository;
import com.bhgroup.pms.repository.PropertyPhotoRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.SeasonalRateRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.mapper.PropertyMapper;
import com.bhgroup.pms.service.mapper.SeasonalRateMapper;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private PropertyPhotoRepository propertyPhotoRepository;
    @Mock private PropertyDocumentRepository propertyDocumentRepository;
    @Mock private SeasonalRateRepository seasonalRateRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private PropertyMapper propertyMapper;
    @Mock private SeasonalRateMapper seasonalRateMapper;

    private PropertyService propertyService;
    private Property property;
    private User actor;

    @BeforeEach
    void setUp() {
        propertyService = new PropertyService(
                propertyRepository, propertyPhotoRepository, propertyDocumentRepository,
                seasonalRateRepository, userRepository, fileStorageService, notificationService,
                auditService, propertyMapper, seasonalRateMapper);

        property = Property.builder().name("Test Apartment").build();
        property.setId(UUID.randomUUID());
        property.setIntegrationMode(IntegrationMode.MANUAL);

        actor = new User();
        actor.setId(UUID.randomUUID());

        // Not every test needs all four - e.g. create() never calls findById.
        lenient().when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        lenient().when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(propertyPhotoRepository.findByPropertyIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        lenient().when(propertyDocumentRepository.findByPropertyIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
    }

    @Test
    void updateIntegrationMode_changesModeAndRecordsAudit() {
        propertyService.updateIntegrationMode(property.getId(), IntegrationMode.ICAL, actor);

        assertThat(property.getIntegrationMode()).isEqualTo(IntegrationMode.ICAL);
        verify(auditService).record(
                eq(AuditAction.PROPERTY_INTEGRATION_MODE_CHANGED), eq(actor), any(), eq(null), eq(null));
    }

    @Test
    void create_rejectsNegativeLateCheckoutFee() {
        var request = createRequest(false, null, new BigDecimal("-10.00"));

        assertThatThrownBy(() -> propertyService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("negativă");
    }

    @Test
    void create_rejectsEnabledLateCheckoutWithoutATime() {
        var request = createRequest(true, null, new BigDecimal("50.00"));

        assertThatThrownBy(() -> propertyService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("oră");
    }

    @Test
    void create_rejectsLateCheckoutTimeNotAfterNormalCheckOutTime() {
        // Default checkOutTime is 11:00 when not specified - 10:00 is before it.
        var request = createRequest(true, LocalTime.of(10, 0), new BigDecimal("50.00"));

        assertThatThrownBy(() -> propertyService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ora normală");
    }

    @Test
    void create_acceptsAValidLateCheckoutConfig() {
        var request = createRequest(true, LocalTime.of(14, 0), new BigDecimal("50.00"));

        assertThatCode(() -> propertyService.create(request)).doesNotThrowAnyException();
    }

    @Test
    void create_acceptsLateCheckoutDisabledRegardlessOfTime() {
        var request = createRequest(false, null, null);

        assertThatCode(() -> propertyService.create(request)).doesNotThrowAnyException();
    }

    private PropertyCreateRequest createRequest(boolean lateCheckoutEnabled, LocalTime lateCheckoutTime,
                                                 BigDecimal lateCheckoutFee) {
        // Positional record - must track PropertyCreateRequest's field order exactly.
        // name, description, propertyType, address, showExactAddressPublicly,
        return new PropertyCreateRequest(
                "Test Apartment", null, PropertyType.APARTMENT, null, false,
                // bedrooms, bathrooms, maxGuests,
                1, 1, 2,
                // sizeSqm, basePricePerNight, weekendPricePerNight, cleaningFee, extraGuestFee,
                // baseGuestsIncluded, weeklyDiscountPercent, monthlyDiscountPercent,
                null, null, null, null, null, null, null, null,
                // minStayNights, maxStayNights, cancellationPolicy, ownerId, commissionPercent,
                // cleaningChecklist, checkInTime, checkOutTime, facilities,
                null, null, null, null, null, null, null, null, null,
                // smartLockEnabled, smartLockProvider, smartLockDeviceId,
                false, null, null,
                // lateCheckoutEnabled, lateCheckoutTime, lateCheckoutFee
                lateCheckoutEnabled, lateCheckoutTime, lateCheckoutFee);
    }
}
