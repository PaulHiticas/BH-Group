package com.bhgroup.pms.common;

/** Shared helpers for keeping personal data out of places that don't need it in full (logs, audit trails). */
public final class PiiMasking {

    private PiiMasking() {
    }

    /** Keeps enough of the address to be useful for support/debugging without exposing it in full. */
    public static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
