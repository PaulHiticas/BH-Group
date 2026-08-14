package com.bhgroup.pms.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class SecureTokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 48;

    // Excludes visually ambiguous characters (0/O, 1/I/L) since these are
    // meant to be typed by hand from a printed/saved list.
    private static final String RECOVERY_CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int RECOVERY_CODE_GROUP_LENGTH = 5;

    public String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Human-typeable one-time code, formatted as e.g. "7K9QX-4NPV2". */
    public String generateRecoveryCode() {
        StringBuilder first = new StringBuilder(RECOVERY_CODE_GROUP_LENGTH);
        StringBuilder second = new StringBuilder(RECOVERY_CODE_GROUP_LENGTH);
        for (int i = 0; i < RECOVERY_CODE_GROUP_LENGTH; i++) {
            first.append(RECOVERY_CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(RECOVERY_CODE_ALPHABET.length())));
            second.append(RECOVERY_CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(RECOVERY_CODE_ALPHABET.length())));
        }
        return first + "-" + second;
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
