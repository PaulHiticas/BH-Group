package com.bhgroup.pms.property;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.property.dto.PropertyCreateRequest;
import com.bhgroup.pms.property.dto.PropertyDocumentResponse;
import com.bhgroup.pms.property.dto.PropertyPhotoResponse;
import com.bhgroup.pms.property.dto.PropertyResponse;
import com.bhgroup.pms.property.dto.PropertySummaryResponse;
import com.bhgroup.pms.property.dto.PropertyUpdateRequest;
import com.bhgroup.pms.security.SecurityUtils;
import com.bhgroup.pms.user.User;
import com.bhgroup.pms.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@Tag(name = "Properties", description = "Property management: CRUD, photos and documents")
public class PropertyController {

    private final PropertyService propertyService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "List properties with search and filters")
    public ResponseEntity<ApiResponse<PageResponse<PropertySummaryResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PropertyStatus status,
            @RequestParam(required = false) PropertyType type,
            Pageable pageable) {
        var result = propertyService.list(search, status, type, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "Get a property by id")
    public ResponseEntity<ApiResponse<PropertyResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "Create a property")
    public ResponseEntity<ApiResponse<PropertyResponse>> create(@Valid @RequestBody PropertyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(propertyService.create(request), "Property created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "Update a property")
    public ResponseEntity<ApiResponse<PropertyResponse>> update(@PathVariable UUID id,
                                                                 @Valid @RequestBody PropertyUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.update(id, request), "Property updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "Delete a property")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        propertyService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Property deleted successfully"));
    }

    @PostMapping(value = "/{id}/photos", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "Upload a property photo")
    public ResponseEntity<ApiResponse<PropertyPhotoResponse>> addPhoto(@PathVariable UUID id,
                                                                        @RequestParam("file") MultipartFile file,
                                                                        @RequestParam(required = false) String caption) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(propertyService.addPhoto(id, file, caption)));
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "Delete a property photo")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(@PathVariable UUID id, @PathVariable UUID photoId) {
        propertyService.deletePhoto(id, photoId);
        return ResponseEntity.ok(ApiResponse.message("Photo deleted successfully"));
    }

    @PatchMapping("/{id}/photos/{photoId}/cover")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "Set a photo as the property cover image")
    public ResponseEntity<ApiResponse<Void>> setCoverPhoto(@PathVariable UUID id, @PathVariable UUID photoId) {
        propertyService.setCoverPhoto(id, photoId);
        return ResponseEntity.ok(ApiResponse.message("Cover photo updated"));
    }

    @PostMapping(value = "/{id}/documents", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "Upload a property document")
    public ResponseEntity<ApiResponse<PropertyDocumentResponse>> addDocument(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam PropertyDocumentType documentType) {
        User currentUser = userRepository.findById(SecurityUtils.requireCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(propertyService.addDocument(id, file, documentType, currentUser)));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "Delete a property document")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID id, @PathVariable UUID documentId) {
        propertyService.deleteDocument(id, documentId);
        return ResponseEntity.ok(ApiResponse.message("Document deleted successfully"));
    }
}
