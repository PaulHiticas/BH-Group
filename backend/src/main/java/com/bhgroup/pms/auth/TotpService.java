package com.bhgroup.pms.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * RFC 6238 (TOTP) implementation compatible with Google Authenticator / Authy.
 */
@Service
public class TotpService {

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int SECRET_BYTE_LENGTH = 20;
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int ALLOWED_WINDOW_STEPS = 1;
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String buildOtpAuthUrl(String secret, String accountEmail, String issuer) {
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String encodedLabel = URLEncoder.encode(issuer + ":" + accountEmail, StandardCharsets.UTF_8);
        return "otpauth://totp/" + encodedLabel
                + "?secret=" + secret
                + "&issuer=" + encodedIssuer
                + "&algorithm=SHA1&digits=" + CODE_DIGITS + "&period=" + TIME_STEP_SECONDS;
    }

    public boolean verifyCode(String base32Secret, String code) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long currentWindow = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        byte[] key = base32Decode(base32Secret);

        for (int i = -ALLOWED_WINDOW_STEPS; i <= ALLOWED_WINDOW_STEPS; i++) {
            String candidate = generateCode(key, currentWindow + i);
            if (candidate.equals(code)) {
                return true;
            }
        }
        return false;
    }

    private String generateCode(byte[] key, long counter) {
        byte[] data = new byte[8];
        long value = counter;
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (value & 0xFF);
            value >>= 8;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Unable to compute TOTP code", ex);
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int bits = 0;
        int value = 0;
        for (byte b : data) {
            value = (value << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                result.append(BASE32_ALPHABET.charAt((value >> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }
        if (bits > 0) {
            result.append(BASE32_ALPHABET.charAt((value << (5 - bits)) & 0x1F));
        }
        return result.toString();
    }

    private byte[] base32Decode(String base32) {
        String sanitized = base32.trim().toUpperCase().replace("=", "");
        int bits = 0;
        int value = 0;
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();

        for (char c : sanitized.toCharArray()) {
            int idx = BASE32_ALPHABET.indexOf(c);
            if (idx < 0) {
                continue;
            }
            value = (value << 5) | idx;
            bits += 5;
            if (bits >= 8) {
                output.write((value >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return output.toByteArray();
    }

    public String base32EncodeForKey(byte[] data) {
        return base32Encode(data);
    }
}
