package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.Facility;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.repository.PropertyPhotoRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.PropertySpecifications;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.dto.publicapi.PublicCalendarEntryResponse;
import com.bhgroup.pms.dto.publicapi.PublicPropertyResponse;
import com.bhgroup.pms.dto.publicapi.PublicPropertySummaryResponse;
import com.bhgroup.pms.repository.ReservationRepository;
import com.bhgroup.pms.domain.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.dto.publicapi.PublicPropertyResponse;
import com.bhgroup.pms.dto.publicapi.PublicPropertySummaryResponse;
import com.bhgroup.pms.repository.PropertyPhotoRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.PropertySpecifications;
import com.bhgroup.pms.repository.ReservationRepository;
import com.bhgroup.pms.service.mapper.PublicPropertyMapper;
@Service
@RequiredArgsConstructor
public class PublicPropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;
    private final ReservationRepository reservationRepository;
    private final PublicPropertyMapper publicPropertyMapper;

    @Transactional(readOnly = true)
    public PageResponse<PublicPropertySummaryResponse> search(String search, Integer guests, Integer bedrooms,
                                                               BigDecimal minPrice, BigDecimal maxPrice,
                                                               Set<Facility> facilities, LocalDate checkIn,
                                                               LocalDate checkOut, Pageable pageable) {
        Specification<Property> spec = PropertySpecifications.combine(
                PropertySpecifications.hasStatus(PropertyStatus.ACTIVE),
                PropertySpecifications.search(search),
                PropertySpecifications.minGuests(guests),
                PropertySpecifications.minBedrooms(bedrooms),
                PropertySpecifications.priceRange(minPrice, maxPrice),
                PropertySpecifications.hasFacilities(facilities)
        );

        Page<Property> page = propertyRepository.findAll(spec, pageable);

        List<Property> filtered = page.getContent();
        if (checkIn != null && checkOut != null) {
            filtered = filtered.stream()
                    .filter(property -> reservationRepository
                            .findOverlapping(property.getId(), checkIn, checkOut, null, ReservationStatus.NON_BLOCKING)
                            .isEmpty())
                    .toList();
        }

        List<PublicPropertySummaryResponse> content = filtered.stream()
                .map(property -> publicPropertyMapper.toSummary(
                        property, propertyPhotoRepository.findByPropertyIdOrderBySortOrderAsc(property.getId())))
                .toList();

        return new PageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast());
    }

    @Transactional(readOnly = true)
    public List<PublicCalendarEntryResponse> calendar(UUID propertyId, LocalDate from, LocalDate to) {
        return reservationRepository.findCalendarEntries(propertyId, from, to, ReservationStatus.NON_BLOCKING).stream()
                .map(r -> new PublicCalendarEntryResponse(r.getCheckInDate(), r.getCheckOutDate()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicPropertyResponse get(UUID id) {
        Property property = propertyRepository.findById(id)
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        return publicPropertyMapper.toResponse(
                property, propertyPhotoRepository.findByPropertyIdOrderBySortOrderAsc(id));
    }
}
