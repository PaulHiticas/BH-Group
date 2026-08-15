package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.dto.pricing.LocalEventCreateRequest;
import com.bhgroup.pms.dto.pricing.LocalEventResponse;
import com.bhgroup.pms.service.LocalEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/local-events")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
@Tag(name = "Local Events", description = "City- or property-scoped events that push dynamic pricing up for their dates")
public class LocalEventController {

    private final LocalEventService localEventService;

    @PostMapping
    @Operation(summary = "Create a local event (must target a city, a property, or both)")
    public ResponseEntity<ApiResponse<LocalEventResponse>> create(@Valid @RequestBody LocalEventCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(localEventService.create(request), "Local event created"));
    }

    @GetMapping
    @Operation(summary = "List local events for a city and/or a property")
    public ResponseEntity<ApiResponse<List<LocalEventResponse>>> list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) UUID propertyId) {
        if (propertyId != null) {
            return ResponseEntity.ok(ApiResponse.success(localEventService.listForProperty(propertyId)));
        }
        return ResponseEntity.ok(ApiResponse.success(localEventService.listForCity(city)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a local event")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        localEventService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Local event deleted"));
    }
}
