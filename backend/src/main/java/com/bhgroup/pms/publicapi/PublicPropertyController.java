package com.bhgroup.pms.publicapi;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.publicapi.dto.PublicPropertyResponse;
import com.bhgroup.pms.publicapi.dto.PublicPropertySummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/properties")
@RequiredArgsConstructor
@Tag(name = "Public Booking Engine", description = "Unauthenticated property search and booking")
public class PublicPropertyController {

    private final PublicPropertyService publicPropertyService;

    @GetMapping
    @Operation(summary = "Search publicly bookable properties")
    public ResponseEntity<ApiResponse<PageResponse<PublicPropertySummaryResponse>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                publicPropertyService.search(search, guests, checkIn, checkOut, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a publicly bookable property by id")
    public ResponseEntity<ApiResponse<PublicPropertyResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(publicPropertyService.get(id)));
    }
}
