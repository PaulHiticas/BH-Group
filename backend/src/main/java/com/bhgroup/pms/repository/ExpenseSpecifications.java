package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.Expense;
import com.bhgroup.pms.domain.ExpenseCategory;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    public static Specification<Expense> hasProperty(UUID propertyId) {
        if (propertyId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("property").get("id"), propertyId);
    }

    public static Specification<Expense> hasPropertyOwner(UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("property").get("owner").get("id"), ownerId);
    }

    public static Specification<Expense> chargeToOwnerOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("chargeToOwner"));
    }

    public static Specification<Expense> hasCategory(ExpenseCategory category) {
        if (category == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Expense> dateFrom(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expenseDate"), from);
    }

    public static Specification<Expense> dateTo(LocalDate to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("expenseDate"), to);
    }

    @SafeVarargs
    public static Specification<Expense> combine(Specification<Expense>... specs) {
        Specification<Expense> result = Specification.where(null);
        for (Specification<Expense> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }
}
