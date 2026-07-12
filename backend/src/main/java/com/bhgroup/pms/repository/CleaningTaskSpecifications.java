package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.CleaningTask;
import com.bhgroup.pms.domain.CleaningTaskStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class CleaningTaskSpecifications {

    private CleaningTaskSpecifications() {
    }

    public static Specification<CleaningTask> hasProperty(UUID propertyId) {
        if (propertyId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("property").get("id"), propertyId);
    }

    public static Specification<CleaningTask> hasStatus(CleaningTaskStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<CleaningTask> hasCleaner(UUID cleanerId) {
        if (cleanerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("assignedCleaner").get("id"), cleanerId);
    }

    @SafeVarargs
    public static Specification<CleaningTask> combine(Specification<CleaningTask>... specs) {
        Specification<CleaningTask> result = Specification.where(null);
        for (Specification<CleaningTask> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }
}
