package com.bhgroup.pms.repository;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

import com.bhgroup.pms.domain.Facility;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.PropertyType;
public final class PropertySpecifications {

    private PropertySpecifications() {
    }

    public static Specification<Property> search(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("address").get("city")), pattern),
                cb.like(cb.lower(root.get("address").get("addressLine")), pattern)
        );
    }

    public static Specification<Property> hasStatus(PropertyStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Property> hasType(PropertyType type) {
        if (type == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("propertyType"), type);
    }

    public static Specification<Property> minGuests(Integer guests) {
        if (guests == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("maxGuests"), guests);
    }

    public static Specification<Property> minBedrooms(Integer bedrooms) {
        if (bedrooms == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("bedrooms"), bedrooms);
    }

    public static Specification<Property> priceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("basePricePerNight"), minPrice, maxPrice);
            }
            if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("basePricePerNight"), minPrice);
            }
            return cb.lessThanOrEqualTo(root.get("basePricePerNight"), maxPrice);
        };
    }

    public static Specification<Property> hasFacilities(Set<Facility> facilities) {
        if (facilities == null || facilities.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Predicate predicate = cb.conjunction();
            for (Facility facility : facilities) {
                predicate = cb.and(predicate, cb.isMember(facility, root.get("facilities")));
            }
            return predicate;
        };
    }

    @SafeVarargs
    public static Specification<Property> combine(Specification<Property>... specs) {
        Specification<Property> result = Specification.where(null);
        for (Specification<Property> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }
}
