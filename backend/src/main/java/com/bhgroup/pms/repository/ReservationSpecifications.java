package com.bhgroup.pms.repository;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationStatus;
public final class ReservationSpecifications {

    private ReservationSpecifications() {
    }

    public static Specification<Reservation> search(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("guestFirstName")), pattern),
                cb.like(cb.lower(root.get("guestLastName")), pattern),
                cb.like(cb.lower(root.get("guestEmail")), pattern)
        );
    }

    public static Specification<Reservation> hasProperty(UUID propertyId) {
        if (propertyId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("property").get("id"), propertyId);
    }

    public static Specification<Reservation> hasPropertyOwner(UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("property").get("owner").get("id"), ownerId);
    }

    public static Specification<Reservation> hasStatus(ReservationStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Reservation> checkInFrom(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("checkInDate"), from);
    }

    public static Specification<Reservation> checkInTo(LocalDate to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("checkInDate"), to);
    }

    public static Specification<Reservation> activeOnly() {
        return (root, query, cb) -> cb.not(root.get("status").in(
                ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW));
    }

    @SafeVarargs
    public static Specification<Reservation> combine(Specification<Reservation>... specs) {
        Specification<Reservation> result = Specification.where(null);
        for (Specification<Reservation> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }
}
