package com.bhgroup.pms.lead;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.lead.dto.LeadCreateRequest;
import com.bhgroup.pms.lead.dto.LeadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Leads", description = "Property owner leads — public submission and admin review")
public class LeadController {

    private final LeadService leadService;

    @PostMapping("/api/v1/public/leads")
    @Operation(summary = "Submit a lead — a property owner interested in listing their property")
    public ResponseEntity<ApiResponse<LeadResponse>> create(@Valid @RequestBody LeadCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(leadService.create(request),
                        "Mulțumim! Te vom contacta în cel mai scurt timp."));
    }

    @GetMapping("/api/v1/leads")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "List property owner leads")
    public ResponseEntity<ApiResponse<PageResponse<LeadResponse>>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(leadService.list(pageable)));
    }

    @PatchMapping("/api/v1/leads/{id}/contacted")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "Mark a lead as contacted or not")
    public ResponseEntity<ApiResponse<LeadResponse>> markContacted(
            @PathVariable UUID id, @RequestParam(defaultValue = "true") boolean contacted) {
        return ResponseEntity.ok(ApiResponse.success(leadService.markContacted(id, contacted)));
    }
}
