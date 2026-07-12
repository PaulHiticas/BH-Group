package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.MaintenanceCategory;
import com.bhgroup.pms.domain.MaintenancePriority;
import com.bhgroup.pms.domain.MaintenanceStatus;
import com.bhgroup.pms.domain.MaintenanceTicket;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class MaintenanceTicketSpecifications {

    private MaintenanceTicketSpecifications() {
    }

    public static Specification<MaintenanceTicket> hasProperty(UUID propertyId) {
        if (propertyId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("property").get("id"), propertyId);
    }

    public static Specification<MaintenanceTicket> hasStatus(MaintenanceStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<MaintenanceTicket> hasPriority(MaintenancePriority priority) {
        if (priority == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<MaintenanceTicket> hasCategory(MaintenanceCategory category) {
        if (category == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<MaintenanceTicket> hasPropertyOwner(UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("property").get("owner").get("id"), ownerId);
    }

    public static Specification<MaintenanceTicket> statusNotIn(java.util.List<MaintenanceStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> cb.not(root.get("status").in(statuses));
    }

    public static Specification<MaintenanceTicket> hasAssignee(UUID assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("assignedTo").get("id"), assigneeId);
    }

    @SafeVarargs
    public static Specification<MaintenanceTicket> combine(Specification<MaintenanceTicket>... specs) {
        Specification<MaintenanceTicket> result = Specification.where(null);
        for (Specification<MaintenanceTicket> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }
}
