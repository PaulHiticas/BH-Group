package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.domain.DynamicPricingConfig;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.dto.pricing.DynamicPricingConfigResponse;
import com.bhgroup.pms.dto.pricing.DynamicPricingConfigUpdateRequest;
import com.bhgroup.pms.repository.DynamicPricingConfigRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DynamicPricingConfigService {

    private final DynamicPricingConfigRepository dynamicPricingConfigRepository;
    private final PropertyRepository propertyRepository;

    @Transactional(readOnly = true)
    public DynamicPricingConfigResponse getOrDefault(UUID propertyId) {
        requireProperty(propertyId);
        return dynamicPricingConfigRepository.findByPropertyId(propertyId)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(defaults(propertyId)));
    }

    @Transactional
    public DynamicPricingConfigResponse update(UUID propertyId, DynamicPricingConfigUpdateRequest request) {
        validate(request);
        Property property = requireProperty(propertyId);

        DynamicPricingConfig config = dynamicPricingConfigRepository.findByPropertyId(propertyId)
                .orElseGet(() -> DynamicPricingConfig.builder().property(property).build());

        config.setEnabled(request.enabled());
        config.setMinPrice(request.minPrice());
        config.setMaxPrice(request.maxPrice());
        config.setOccupancyWindowDays(request.occupancyWindowDays());
        config.setOccupancyMultiplierMin(request.occupancyMultiplierMin());
        config.setOccupancyMultiplierMax(request.occupancyMultiplierMax());
        config.setLeadTimeDays(request.leadTimeDays());
        config.setLeadTimeMultiplier(request.leadTimeMultiplier());

        return toResponse(dynamicPricingConfigRepository.save(config));
    }

    private void validate(DynamicPricingConfigUpdateRequest request) {
        if (request.minPrice() != null && request.maxPrice() != null
                && request.minPrice().compareTo(request.maxPrice()) > 0) {
            throw new BadRequestException("Minimum price cannot be greater than maximum price");
        }
        if (request.occupancyMultiplierMax().compareTo(request.occupancyMultiplierMin()) < 0) {
            throw new BadRequestException("Maximum occupancy multiplier cannot be lower than the minimum");
        }
    }

    private Property requireProperty(UUID propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
    }

    private DynamicPricingConfig defaults(UUID propertyId) {
        Property property = new Property();
        property.setId(propertyId);
        return DynamicPricingConfig.builder().property(property).build();
    }

    private DynamicPricingConfigResponse toResponse(DynamicPricingConfig config) {
        return new DynamicPricingConfigResponse(
                config.getProperty().getId(),
                config.isEnabled(),
                config.getMinPrice(),
                config.getMaxPrice(),
                config.getOccupancyWindowDays(),
                config.getOccupancyMultiplierMin(),
                config.getOccupancyMultiplierMax(),
                config.getLeadTimeDays(),
                config.getLeadTimeMultiplier());
    }
}
