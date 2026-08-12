package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.OwnerStatement;
import com.bhgroup.pms.domain.OwnerStatementStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class OwnerStatementSpecifications {

    private OwnerStatementSpecifications() {
    }

    public static Specification<OwnerStatement> hasOwner(UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<OwnerStatement> hasStatus(OwnerStatementStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    @SafeVarargs
    public static Specification<OwnerStatement> combine(Specification<OwnerStatement>... specs) {
        Specification<OwnerStatement> result = Specification.where(null);
        for (Specification<OwnerStatement> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }
}
