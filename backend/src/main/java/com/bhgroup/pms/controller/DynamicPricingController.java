package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.dto.pricing.DynamicPriceBreakdownResponse;
import com.bhgroup.pms.dto.pricing.DynamicPricingConfigResponse;
import com.bhgroup.pms.dto.pricing.DynamicPricingConfigUpdateRequest;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.service.DynamicPricingConfigService;
import com.bhgroup.pms.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}/pricing")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
@Tag(name = "Dynamic Pricing", description = "Occupancy/lead-time/event based pricing on top of seasonal rates")
public class DynamicPricingController {

    private final DynamicPricingConfigService dynamicPricingConfigService;
    private final PricingService pricingService;
    private final PropertyRepository propertyRepository;

    @GetMapping("/config")
    @Operation(summary = "Get the property's dynamic pricing configuration (defaults if never configured)")
    public ResponseEntity<ApiResponse<DynamicPricingConfigResponse>> getConfig(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.success(dynamicPricingConfigService.getOrDefault(propertyId)));
    }

    @PutMapping("/config")
    @Operation(summary = "Update the property's dynamic pricing configuration")
    public ResponseEntity<ApiResponse<DynamicPricingConfigResponse>> updateConfig(
            @PathVariable UUID propertyId, @Valid @RequestBody DynamicPricingConfigUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                dynamicPricingConfigService.update(propertyId, request), "Dynamic pricing configuration updated"));
    }

    @GetMapping("/breakdown")
    @Operation(summary = "Auditable per-night price breakdown: base rate + every dynamic factor + final rate")
    public ResponseEntity<ApiResponse<DynamicPriceBreakdownResponse>> breakdown(
            @PathVariable UUID propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int guests) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        return ResponseEntity.ok(ApiResponse.success(
                pricingService.priceBreakdown(property, checkIn, checkOut, guests)));
    }
}
