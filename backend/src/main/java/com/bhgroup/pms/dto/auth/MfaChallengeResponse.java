package com.bhgroup.pms.dto.auth;

public record MfaChallengeResponse(
        String challengeToken,
        long expiresIn
) {
}
