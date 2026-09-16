package com.bhstays.pms.dto.auth;

public record MfaChallengeResponse(
        String challengeToken,
        long expiresIn
) {
}
