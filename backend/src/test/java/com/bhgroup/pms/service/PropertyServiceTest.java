package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.domain.AuditAction;
import com.bhgroup.pms.domain.IntegrationMode;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.property.PropertyResponse;
import com.bhgroup.pms.repository.PropertyDocumentRepository;
import com.bhgroup.pms.repository.PropertyPhotoRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.SeasonalRateRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.mapper.PropertyMapper;
import com.bhgroup.pms.service.mapper.SeasonalRateMapper;
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

        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(propertyPhotoRepository.findByPropertyIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(propertyDocumentRepository.findByPropertyIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
    }

    @Test
    void updateIntegrationMode_changesModeAndRecordsAudit() {
        propertyService.updateIntegrationMode(property.getId(), IntegrationMode.ICAL, actor);

        assertThat(property.getIntegrationMode()).isEqualTo(IntegrationMode.ICAL);
        verify(auditService).record(
                eq(AuditAction.PROPERTY_INTEGRATION_MODE_CHANGED), eq(actor), any(), eq(null), eq(null));
    }
}
