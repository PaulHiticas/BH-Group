package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.domain.LocalEvent;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.dto.pricing.LocalEventCreateRequest;
import com.bhgroup.pms.dto.pricing.LocalEventResponse;
import com.bhgroup.pms.repository.LocalEventRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocalEventService {

    private final LocalEventRepository localEventRepository;
    private final PropertyRepository propertyRepository;

    @Transactional
    public LocalEventResponse create(LocalEventCreateRequest request) {
        boolean hasCity = request.city() != null && !request.city().isBlank();
        boolean hasProperty = request.propertyId() != null;
        if (!hasCity && !hasProperty) {
            throw new BadRequestException("Either a city or a property must be specified");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        Property property = null;
        if (hasProperty) {
            property = propertyRepository.findById(request.propertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        }

        LocalEvent event = LocalEvent.builder()
                .city(hasCity ? request.city().trim() : null)
                .property(property)
                .label(request.label())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .priceMultiplier(request.priceMultiplier())
                .build();

        return toResponse(localEventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<LocalEventResponse> listForCity(String city) {
        String normalized = city == null ? "" : city.trim().toLowerCase(Locale.ROOT);
        return localEventRepository.findByNormalizedCity(normalized).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LocalEventResponse> listForProperty(UUID propertyId) {
        return localEventRepository.findByPropertyIdOrderByStartDateAsc(propertyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID id) {
        if (!localEventRepository.existsById(id)) {
            throw new ResourceNotFoundException("Local event not found");
        }
        localEventRepository.deleteById(id);
    }

    private LocalEventResponse toResponse(LocalEvent event) {
        return new LocalEventResponse(
                event.getId(),
                event.getCity(),
                event.getProperty() != null ? event.getProperty().getId() : null,
                event.getProperty() != null ? event.getProperty().getName() : null,
                event.getLabel(),
                event.getStartDate(),
                event.getEndDate(),
                event.getPriceMultiplier(),
                event.getCreatedAt());
    }
}
