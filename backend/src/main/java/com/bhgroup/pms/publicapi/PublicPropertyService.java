package com.bhgroup.pms.publicapi;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.property.Property;
import com.bhgroup.pms.property.PropertyPhotoRepository;
import com.bhgroup.pms.property.PropertyRepository;
import com.bhgroup.pms.property.PropertySpecifications;
import com.bhgroup.pms.property.PropertyStatus;
import com.bhgroup.pms.publicapi.dto.PublicPropertyResponse;
import com.bhgroup.pms.publicapi.dto.PublicPropertySummaryResponse;
import com.bhgroup.pms.reservation.ReservationRepository;
import com.bhgroup.pms.reservation.ReservationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicPropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;
    private final ReservationRepository reservationRepository;
    private final PublicPropertyMapper publicPropertyMapper;

    @Transactional(readOnly = true)
    public PageResponse<PublicPropertySummaryResponse> search(String search, Integer guests, LocalDate checkIn,
                                                               LocalDate checkOut, Pageable pageable) {
        Specification<Property> spec = PropertySpecifications.combine(
                PropertySpecifications.hasStatus(PropertyStatus.ACTIVE),
                PropertySpecifications.search(search),
                PropertySpecifications.minGuests(guests)
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
    public PublicPropertyResponse get(UUID id) {
        Property property = propertyRepository.findById(id)
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        return publicPropertyMapper.toResponse(
                property, propertyPhotoRepository.findByPropertyIdOrderBySortOrderAsc(id));
    }
}
