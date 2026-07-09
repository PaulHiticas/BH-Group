package com.bhgroup.pms.reservation;

import java.util.Set;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED,
    NO_SHOW;

    /**
     * Statuses that do not occupy the property's calendar (excluded from
     * overlap/availability checks and revenue calculations).
     */
    public static final Set<ReservationStatus> NON_BLOCKING = Set.of(CANCELLED, NO_SHOW);
}
