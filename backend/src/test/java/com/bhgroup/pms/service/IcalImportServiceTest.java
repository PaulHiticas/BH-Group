package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.domain.IcalImportFeed;
import com.bhgroup.pms.domain.IntegrationMode;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.ReservationSource;
import com.bhgroup.pms.dto.ical.IcalImportFeedCreateRequest;
import com.bhgroup.pms.repository.IcalImportFeedRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import com.bhgroup.pms.service.mapper.IcalImportFeedMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IcalImportServiceTest {

    @Mock
    private IcalImportFeedRepository icalImportFeedRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private IcalImportFeedMapper icalImportFeedMapper;

    private IcalImportService icalImportService;
    private Property property;

    @BeforeEach
    void setUp() {
        icalImportService = new IcalImportService(
                icalImportFeedRepository, propertyRepository, reservationRepository, icalImportFeedMapper);
        property = Property.builder().name("Test Apartment").build();
        property.setId(UUID.randomUUID());
    }

    @Test
    void addFeed_rejectsWhenPropertyNotInIcalMode() {
        property.setIntegrationMode(IntegrationMode.MANUAL);
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));

        IcalImportFeedCreateRequest request = new IcalImportFeedCreateRequest(
                ReservationSource.AIRBNB, "https://example.com/feed.ics");

        assertThatThrownBy(() -> icalImportService.addFeed(property.getId(), request))
                .isInstanceOf(BadRequestException.class);

        verify(icalImportFeedRepository, never()).save(any());
    }

    @Test
    void addFeed_rejectsAFeedUrlPointingAtAnInternalAddress() {
        property.setIntegrationMode(IntegrationMode.ICAL);
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));

        IcalImportFeedCreateRequest request = new IcalImportFeedCreateRequest(
                ReservationSource.AIRBNB, "http://169.254.169.254/latest/meta-data/");

        assertThatThrownBy(() -> icalImportService.addFeed(property.getId(), request))
                .isInstanceOf(BadRequestException.class);

        verify(icalImportFeedRepository, never()).save(any());
    }

    @Test
    void addFeed_allowedWhenPropertyInIcalMode() {
        property.setIntegrationMode(IntegrationMode.ICAL);
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(icalImportFeedRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // A literal public IP (RFC 5737 TEST-NET-3), not a hostname - the URL
        // validator now resolves the host via DNS, so this keeps the test
        // network-independent instead of depending on real DNS resolution.
        IcalImportFeedCreateRequest request = new IcalImportFeedCreateRequest(
                ReservationSource.AIRBNB, "https://203.0.113.10/feed.ics");

        icalImportService.addFeed(property.getId(), request);

        verify(icalImportFeedRepository).save(any());
    }

    @Test
    void syncFeed_rejectsWhenPropertyNotInIcalMode() {
        property.setIntegrationMode(IntegrationMode.CHANNEL_MANAGER);
        UUID feedId = UUID.randomUUID();
        IcalImportFeed feed = IcalImportFeed.builder()
                .property(property)
                .source(ReservationSource.AIRBNB)
                .feedUrl("https://example.com/feed.ics")
                .build();
        feed.setId(feedId);
        when(icalImportFeedRepository.findById(feedId)).thenReturn(Optional.of(feed));

        assertThatThrownBy(() -> icalImportService.syncFeed(property.getId(), feedId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void syncFeed_throwsResourceNotFoundWhenFeedBelongsToDifferentProperty() {
        UUID feedId = UUID.randomUUID();
        Property otherProperty = Property.builder().name("Other").build();
        otherProperty.setId(UUID.randomUUID());
        IcalImportFeed feed = IcalImportFeed.builder()
                .property(otherProperty)
                .source(ReservationSource.AIRBNB)
                .feedUrl("https://example.com/feed.ics")
                .build();
        feed.setId(feedId);
        when(icalImportFeedRepository.findById(feedId)).thenReturn(Optional.of(feed));

        assertThatThrownBy(() -> icalImportService.syncFeed(property.getId(), feedId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void syncAllFeeds_skipsFeedsForPropertiesNotInIcalMode() {
        property.setIntegrationMode(IntegrationMode.MANUAL);
        IcalImportFeed feed = IcalImportFeed.builder()
                .property(property)
                .source(ReservationSource.AIRBNB)
                .feedUrl("https://example.com/feed.ics")
                .build();
        feed.setId(UUID.randomUUID());
        when(icalImportFeedRepository.findAll()).thenReturn(List.of(feed));

        icalImportService.syncAllFeeds();

        verify(icalImportFeedRepository, never()).save(any());
    }
}
