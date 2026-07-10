package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.dto.property.AddressDto;
import com.bhgroup.pms.dto.property.PropertyDocumentResponse;
import com.bhgroup.pms.dto.property.PropertyPhotoResponse;
import com.bhgroup.pms.dto.property.PropertyResponse;
import com.bhgroup.pms.dto.property.PropertySummaryResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

import com.bhgroup.pms.domain.Address;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyDocument;
import com.bhgroup.pms.domain.PropertyPhoto;
import com.bhgroup.pms.dto.property.AddressDto;
import com.bhgroup.pms.dto.property.PropertyDocumentResponse;
import com.bhgroup.pms.dto.property.PropertyPhotoResponse;
import com.bhgroup.pms.dto.property.PropertyResponse;
import com.bhgroup.pms.dto.property.PropertySummaryResponse;
@Component
public class PropertyMapper {

    public PropertyResponse toResponse(Property property, List<PropertyPhoto> photos, List<PropertyDocument> documents) {
        return new PropertyResponse(
                property.getId(),
                property.getName(),
                property.getDescription(),
                property.getPropertyType(),
                property.getStatus(),
                toAddressDto(property.getAddress()),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getMaxGuests(),
                property.getSizeSqm(),
                property.getBasePricePerNight(),
                property.getCheckInTime(),
                property.getCheckOutTime(),
                property.getFacilities(),
                property.isSmartLockEnabled(),
                property.getSmartLockProvider(),
                property.getSmartLockDeviceId(),
                photos.stream().map(this::toPhotoResponse).toList(),
                documents.stream().map(this::toDocumentResponse).toList(),
                property.getCreatedAt(),
                property.getUpdatedAt()
        );
    }

    public PropertySummaryResponse toSummaryResponse(Property property, List<PropertyPhoto> photos) {
        String coverUrl = photos.stream()
                .sorted(Comparator.comparing(PropertyPhoto::isCover, Comparator.reverseOrder())
                        .thenComparingInt(PropertyPhoto::getSortOrder))
                .map(PropertyPhoto::getUrl)
                .findFirst()
                .orElse(null);

        return new PropertySummaryResponse(
                property.getId(),
                property.getName(),
                property.getAddress().getCity(),
                property.getPropertyType(),
                property.getStatus(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getMaxGuests(),
                coverUrl,
                property.getCreatedAt()
        );
    }

    public PropertyPhotoResponse toPhotoResponse(PropertyPhoto photo) {
        return new PropertyPhotoResponse(
                photo.getId(), photo.getUrl(), photo.getCaption(), photo.getSortOrder(), photo.isCover());
    }

    public PropertyDocumentResponse toDocumentResponse(PropertyDocument document) {
        return new PropertyDocumentResponse(
                document.getId(), document.getFileName(), document.getUrl(),
                document.getDocumentType(), document.getCreatedAt());
    }

    public Address toAddress(AddressDto dto) {
        return new Address(
                dto.addressLine(), dto.city(), dto.county(), dto.postalCode(),
                dto.country(), dto.latitude(), dto.longitude());
    }

    private AddressDto toAddressDto(Address address) {
        return new AddressDto(
                address.getAddressLine(), address.getCity(), address.getCounty(), address.getPostalCode(),
                address.getCountry(), address.getLatitude(), address.getLongitude());
    }
}
