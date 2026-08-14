package com.bhgroup.pms.common;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Shared helpers for keeping personal data out of places that don't need it in full (logs, audit trails). */
public final class PiiMasking {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private PiiMasking() {
    }

    /**
     * Keeps enough of the address to be useful for support/debugging without
     * exposing it in full. Anything that isn't a well-formed "x@y" shape
     * masks to a fixed, fully-opaque value rather than leaking most of the
     * input back out unmasked.
     */
    public static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /**
     * A deterministic, non-reversible fingerprint for "is this the same
     * email as an earlier request" without storing or logging the email
     * itself - {@link #maskEmail} alone isn't unique enough for that (many
     * addresses can share a first character and domain). Keyed with an
     * application secret (not used bare) so the fingerprint can't be
     * brute-forced from a list of common addresses.
     */
    public static String fingerprintEmail(String email, String applicationSecret) {
        if (email == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    ("gdpr-email-fingerprint:" + applicationSecret).getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(email.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Failed to fingerprint email", ex);
        }
    }
}
