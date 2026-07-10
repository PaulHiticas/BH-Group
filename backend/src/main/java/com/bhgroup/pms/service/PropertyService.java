package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.dto.property.PropertyCreateRequest;
import com.bhgroup.pms.dto.property.PropertyDocumentResponse;
import com.bhgroup.pms.dto.property.PropertyPhotoResponse;
import com.bhgroup.pms.dto.property.PropertyResponse;
import com.bhgroup.pms.dto.property.PropertySummaryResponse;
import com.bhgroup.pms.dto.property.PropertyUpdateRequest;
import com.bhgroup.pms.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyDocument;
import com.bhgroup.pms.domain.PropertyDocumentType;
import com.bhgroup.pms.domain.PropertyPhoto;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.PropertyType;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.property.PropertyCreateRequest;
import com.bhgroup.pms.dto.property.PropertyDocumentResponse;
import com.bhgroup.pms.dto.property.PropertyPhotoResponse;
import com.bhgroup.pms.dto.property.PropertyResponse;
import com.bhgroup.pms.dto.property.PropertySummaryResponse;
import com.bhgroup.pms.dto.property.PropertyUpdateRequest;
import com.bhgroup.pms.repository.PropertyDocumentRepository;
import com.bhgroup.pms.repository.PropertyPhotoRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.PropertySpecifications;
import com.bhgroup.pms.service.mapper.PropertyMapper;
@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;
    private final PropertyDocumentRepository propertyDocumentRepository;
    private final FileStorageService fileStorageService;
    private final PropertyMapper propertyMapper;

    @Transactional(readOnly = true)
    public PageResponse<PropertySummaryResponse> list(String search, PropertyStatus status, PropertyType type,
                                                        Pageable pageable) {
        Specification<Property> spec = PropertySpecifications.combine(
                PropertySpecifications.search(search),
                PropertySpecifications.hasStatus(status),
                PropertySpecifications.hasType(type)
        );

        Page<Property> page = propertyRepository.findAll(spec, pageable);
        return PageResponse.of(page, property -> propertyMapper.toSummaryResponse(
                property, propertyPhotoRepository.findByPropertyIdOrderBySortOrderAsc(property.getId())));
    }

    @Transactional(readOnly = true)
    public PropertyResponse get(UUID id) {
        Property property = findPropertyOrThrow(id);
        return toFullResponse(property);
    }

    @Transactional(readOnly = true)
    public List<List<String>> exportRows(String search, PropertyStatus status, PropertyType type) {
        Specification<Property> spec = PropertySpecifications.combine(
                PropertySpecifications.search(search),
                PropertySpecifications.hasStatus(status),
                PropertySpecifications.hasType(type)
        );

        return propertyRepository.findAll(spec, org.springframework.data.domain.Sort.by("name")).stream()
                .map(property -> List.of(
                        property.getName(),
                        property.getPropertyType().name(),
                        property.getStatus().name(),
                        property.getAddress().getCity(),
                        property.getAddress().getAddressLine(),
                        String.valueOf(property.getBedrooms()),
                        String.valueOf(property.getBathrooms()),
                        String.valueOf(property.getMaxGuests()),
                        property.getBasePricePerNight() != null ? property.getBasePricePerNight().toString() : ""
                ))
                .toList();
    }

    @Transactional
    public PropertyResponse create(PropertyCreateRequest request) {
        Property property = Property.builder()
                .name(request.name())
                .description(request.description())
                .propertyType(request.propertyType())
                .status(PropertyStatus.DRAFT)
                .address(propertyMapper.toAddress(request.address()))
                .bedrooms(request.bedrooms())
                .bathrooms(request.bathrooms())
                .maxGuests(request.maxGuests())
                .sizeSqm(request.sizeSqm())
                .basePricePerNight(request.basePricePerNight())
                .checkInTime(request.checkInTime() != null ? request.checkInTime() : java.time.LocalTime.of(14, 0))
                .checkOutTime(request.checkOutTime() != null ? request.checkOutTime() : java.time.LocalTime.of(11, 0))
                .facilities(request.facilities() != null ? request.facilities() : java.util.Set.of())
                .smartLockEnabled(request.smartLockEnabled())
                .smartLockProvider(request.smartLockProvider())
                .smartLockDeviceId(request.smartLockDeviceId())
                .build();

        property = propertyRepository.save(property);
        return toFullResponse(property);
    }

    @Transactional
    public PropertyResponse update(UUID id, PropertyUpdateRequest request) {
        Property property = findPropertyOrThrow(id);

        property.setName(request.name());
        property.setDescription(request.description());
        property.setPropertyType(request.propertyType());
        property.setStatus(request.status());
        property.setAddress(propertyMapper.toAddress(request.address()));
        property.setBedrooms(request.bedrooms());
        property.setBathrooms(request.bathrooms());
        property.setMaxGuests(request.maxGuests());
        property.setSizeSqm(request.sizeSqm());
        property.setBasePricePerNight(request.basePricePerNight());
        if (request.checkInTime() != null) property.setCheckInTime(request.checkInTime());
        if (request.checkOutTime() != null) property.setCheckOutTime(request.checkOutTime());
        property.setFacilities(request.facilities() != null ? request.facilities() : java.util.Set.of());
        property.setSmartLockEnabled(request.smartLockEnabled());
        property.setSmartLockProvider(request.smartLockProvider());
        property.setSmartLockDeviceId(request.smartLockDeviceId());

        property = propertyRepository.save(property);
        return toFullResponse(property);
    }

    @Transactional
    public void delete(UUID id) {
        Property property = findPropertyOrThrow(id);
        List<PropertyPhoto> photos = propertyPhotoRepository.findByPropertyIdOrderBySortOrderAsc(id);
        List<PropertyDocument> documents = propertyDocumentRepository.findByPropertyIdOrderByCreatedAtDesc(id);

        photos.forEach(photo -> fileStorageService.delete(photo.getFileKey()));
        documents.forEach(document -> fileStorageService.delete(document.getFileKey()));

        propertyRepository.delete(property);
    }

    @Transactional
    public PropertyPhotoResponse addPhoto(UUID propertyId, MultipartFile file, String caption) {
        Property property = findPropertyOrThrow(propertyId);
        FileStorageService.StoredFile stored = fileStorageService.storeImage(file, "properties/" + propertyId);

        boolean isFirstPhoto = propertyPhotoRepository.countByPropertyId(propertyId) == 0;
        int nextSortOrder = (int) propertyPhotoRepository.countByPropertyId(propertyId);

        PropertyPhoto photo = PropertyPhoto.builder()
                .property(property)
                .fileKey(stored.fileKey())
                .url(stored.url())
                .caption(caption)
                .sortOrder(nextSortOrder)
                .cover(isFirstPhoto)
                .build();

        photo = propertyPhotoRepository.save(photo);
        return propertyMapper.toPhotoResponse(photo);
    }

    @Transactional
    public void deletePhoto(UUID propertyId, UUID photoId) {
        PropertyPhoto photo = propertyPhotoRepository.findById(photoId)
                .filter(p -> p.getProperty().getId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found"));

        boolean wasCover = photo.isCover();
        fileStorageService.delete(photo.getFileKey());
        propertyPhotoRepository.delete(photo);

        if (wasCover) {
            propertyPhotoRepository.findByPropertyIdOrderBySortOrderAsc(propertyId).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setCover(true);
                        propertyPhotoRepository.save(next);
                    });
        }
    }

    @Transactional
    public void setCoverPhoto(UUID propertyId, UUID photoId) {
        List<PropertyPhoto> photos = propertyPhotoRepository.findByPropertyIdOrderBySortOrderAsc(propertyId);
        boolean found = false;
        for (PropertyPhoto photo : photos) {
            boolean isTarget = photo.getId().equals(photoId);
            photo.setCover(isTarget);
            found = found || isTarget;
        }
        if (!found) {
            throw new ResourceNotFoundException("Photo not found");
        }
        propertyPhotoRepository.saveAll(photos);
    }

    @Transactional
    public PropertyDocumentResponse addDocument(UUID propertyId, MultipartFile file,
                                                 PropertyDocumentType documentType, User uploadedBy) {
        Property property = findPropertyOrThrow(propertyId);
        FileStorageService.StoredFile stored = fileStorageService.storeDocument(file, "properties/" + propertyId + "/documents");

        PropertyDocument document = PropertyDocument.builder()
                .property(property)
                .fileName(file.getOriginalFilename())
                .fileKey(stored.fileKey())
                .url(stored.url())
                .documentType(documentType)
                .uploadedBy(uploadedBy)
                .createdAt(Instant.now())
                .build();

        document = propertyDocumentRepository.save(document);
        return propertyMapper.toDocumentResponse(document);
    }

    @Transactional
    public void deleteDocument(UUID propertyId, UUID documentId) {
        PropertyDocument document = propertyDocumentRepository.findById(documentId)
                .filter(d -> d.getProperty().getId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        fileStorageService.delete(document.getFileKey());
        propertyDocumentRepository.delete(document);
    }

    private PropertyResponse toFullResponse(Property property) {
        List<PropertyPhoto> photos = propertyPhotoRepository.findByPropertyIdOrderBySortOrderAsc(property.getId());
        List<PropertyDocument> documents = propertyDocumentRepository.findByPropertyIdOrderByCreatedAtDesc(property.getId());
        return propertyMapper.toResponse(property, photos, documents);
    }

    private Property findPropertyOrThrow(UUID id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
    }

}
