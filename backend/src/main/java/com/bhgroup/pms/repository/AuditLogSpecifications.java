package com.bhgroup.pms.repository;

import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

import com.bhgroup.pms.domain.AuditLog;
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> hasEntityName(String entityName) {
        if (entityName == null || entityName.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("entityName"), entityName);
    }

    public static Specification<AuditLog> hasAction(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> actorEmailContains(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(cb.lower(root.get("actorEmail")), "%" + actorEmail.toLowerCase() + "%");
    }

    public static Specification<AuditLog> createdFrom(Instant from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<AuditLog> createdTo(Instant to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    @SafeVarargs
    public static Specification<AuditLog> combine(Specification<AuditLog>... specs) {
        Specification<AuditLog> result = Specification.where(null);
        for (Specification<AuditLog> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }
}
