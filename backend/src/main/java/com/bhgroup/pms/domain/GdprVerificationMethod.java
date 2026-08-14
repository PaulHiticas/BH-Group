package com.bhgroup.pms.domain;

/** How the admin confirmed the requester is who they claim to be before acting on their data. */
public enum GdprVerificationMethod {
    EMAIL_CONFIRMATION,
    RESERVATION_DETAILS,
    IDENTITY_DOCUMENT,
    OTHER
}
