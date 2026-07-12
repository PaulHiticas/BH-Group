package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.SeasonalRate;
import com.bhgroup.pms.dto.property.PropertyCreateRequest;
import com.bhgroup.pms.dto.property.PropertyDocumentResponse;
import com.bhgroup.pms.dto.property.PropertyPhotoResponse;
import com.bhgroup.pms.dto.property.PropertyResponse;
import com.bhgroup.pms.dto.property.PropertySummaryResponse;
import com.bhgroup.pms.dto.property.PropertyUpdateRequest;
import com.bhgroup.pms.dto.property.SeasonalRateRequest;
import com.bhgroup.pms.dto.property.SeasonalRateResponse;
import com.bhgroup.pms.repository.SeasonalRateRepository;
import com.bhgroup.pms.service.mapper.SeasonalRateMapper;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final int DOCUMENT_EXPIRY_WARNING_DAYS = 30;

    private final PropertyRepository propertyRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;
    private final PropertyDocumentRepository propertyDocumentRepository;
    private final SeasonalRateRepository seasonalRateRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final PropertyMapper propertyMapper;
    private final SeasonalRateMapper seasonalRateMapper;

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
                .weekendPricePerNight(request.weekendPricePerNight())
                .cleaningFee(request.cleaningFee())
                .extraGuestFee(request.extraGuestFee())
                .baseGuestsIncluded(request.baseGuestsIncluded())
                .weeklyDiscountPercent(request.weeklyDiscountPercent())
                .monthlyDiscountPercent(request.monthlyDiscountPercent())
                .minStayNights(request.minStayNights())
                .maxStayNights(request.maxStayNights())
                .cancellationPolicy(request.cancellationPolicy() != null
                        ? request.cancellationPolicy() : com.bhgroup.pms.domain.CancellationPolicy.MODERATE)
                .owner(resolveOwner(request.ownerId()))
                .commissionPercent(request.commissionPercent())
                .cleaningChecklist(request.cleaningChecklist() != null
                        ? new java.util.ArrayList<>(request.cleaningChecklist()) : new java.util.ArrayList<>())
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
        property.setWeekendPricePerNight(request.weekendPricePerNight());
        property.setCleaningFee(request.cleaningFee());
        property.setExtraGuestFee(request.extraGuestFee());
        property.setBaseGuestsIncluded(request.baseGuestsIncluded());
        property.setWeeklyDiscountPercent(request.weeklyDiscountPercent());
        property.setMonthlyDiscountPercent(request.monthlyDiscountPercent());
        property.setMinStayNights(request.minStayNights());
        property.setMaxStayNights(request.maxStayNights());
        if (request.cancellationPolicy() != null) property.setCancellationPolicy(request.cancellationPolicy());
        property.setOwner(resolveOwner(request.ownerId()));
        property.setCommissionPercent(request.commissionPercent());
        property.setCleaningChecklist(request.cleaningChecklist() != null
                ? new java.util.ArrayList<>(request.cleaningChecklist()) : new java.util.ArrayList<>());
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
                                                 PropertyDocumentType documentType, User uploadedBy,
                                                 java.time.LocalDate expiresAt) {
        Property property = findPropertyOrThrow(propertyId);
        FileStorageService.StoredFile stored = fileStorageService.storeDocument(file, "properties/" + propertyId + "/documents");

        PropertyDocument document = PropertyDocument.builder()
                .property(property)
                .fileName(file.getOriginalFilename())
                .fileKey(stored.fileKey())
                .url(stored.url())
                .documentType(documentType)
                .uploadedBy(uploadedBy)
                .expiresAt(expiresAt)
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

    @Transactional(readOnly = true)
    public List<SeasonalRateResponse> listSeasonalRates(UUID propertyId) {
        findPropertyOrThrow(propertyId);
        return seasonalRateRepository.findByPropertyIdOrderByStartDateAsc(propertyId).stream()
                .map(seasonalRateMapper::toResponse)
                .toList();
    }

    @Transactional
    public SeasonalRateResponse addSeasonalRate(UUID propertyId, SeasonalRateRequest request) {
        Property property = findPropertyOrThrow(propertyId);
        validateSeasonalRateDates(request);

        SeasonalRate rate = SeasonalRate.builder()
                .property(property)
                .label(request.label())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .pricePerNight(request.pricePerNight())
                .build();

        return seasonalRateMapper.toResponse(saveGuardingOverlap(rate));
    }

    @Transactional
    public SeasonalRateResponse updateSeasonalRate(UUID propertyId, UUID rateId, SeasonalRateRequest request) {
        SeasonalRate rate = findSeasonalRateOrThrow(propertyId, rateId);
        validateSeasonalRateDates(request);

        rate.setLabel(request.label());
        rate.setStartDate(request.startDate());
        rate.setEndDate(request.endDate());
        rate.setPricePerNight(request.pricePerNight());

        return seasonalRateMapper.toResponse(saveGuardingOverlap(rate));
    }

    @Transactional
    public void deleteSeasonalRate(UUID propertyId, UUID rateId) {
        seasonalRateRepository.delete(findSeasonalRateOrThrow(propertyId, rateId));
    }

    private void validateSeasonalRateDates(SeasonalRateRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("End date must be on or after the start date");
        }
    }

    private SeasonalRate saveGuardingOverlap(SeasonalRate rate) {
        try {
            return seasonalRateRepository.saveAndFlush(rate);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("This date range overlaps an existing season for this property");
        }
    }

    private SeasonalRate findSeasonalRateOrThrow(UUID propertyId, UUID rateId) {
        return seasonalRateRepository.findById(rateId)
                .filter(r -> r.getProperty().getId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Seasonal rate not found"));
    }

    private PropertyResponse toFullResponse(Property property) {
        List<PropertyPhoto> photos = propertyPhotoRepository.findByPropertyIdOrderBySortOrderAsc(property.getId());
        List<PropertyDocument> documents = propertyDocumentRepository.findByPropertyIdOrderByCreatedAtDesc(property.getId());
        return propertyMapper.toResponse(property, photos, documents);
    }

    private User resolveOwner(UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new com.bhgroup.pms.common.exception.BadRequestException("Owner not found"));
        if (owner.getRole() != Role.OWNER) {
            throw new com.bhgroup.pms.common.exception.BadRequestException("Assigned user must have the OWNER role");
        }
        return owner;
    }

    private Property findPropertyOrThrow(UUID id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
    }

    /**
     * Sends a one-time in-app reminder for each property document expiring
     * within {@link #DOCUMENT_EXPIRY_WARNING_DAYS} days (or already expired)
     * that hasn't been notified about yet.
     */
    @Transactional
    public void notifyExpiringDocuments() {
        java.time.LocalDate cutoff = java.time.LocalDate.now().plusDays(DOCUMENT_EXPIRY_WARNING_DAYS);
        List<PropertyDocument> expiring =
                propertyDocumentRepository.findByExpiresAtLessThanEqualAndExpiryNotifiedAtIsNull(cutoff);

        for (PropertyDocument document : expiring) {
            boolean alreadyExpired = document.getExpiresAt().isBefore(java.time.LocalDate.now());
            notificationService.notifyAdmins(
                    com.bhgroup.pms.domain.NotificationType.DOCUMENT_EXPIRING,
                    (alreadyExpired ? "Document expirat: " : "Document expiră curând: ") + document.getFileName(),
                    document.getProperty().getName() + " — expiră la " + document.getExpiresAt(),
                    "/dashboard/properties/" + document.getProperty().getId());
            document.setExpiryNotifiedAt(Instant.now());
        }
        propertyDocumentRepository.saveAll(expiring);
    }
}
