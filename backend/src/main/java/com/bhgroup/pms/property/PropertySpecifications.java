package com.bhgroup.pms.property;

import org.springframework.data.jpa.domain.Specification;

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
