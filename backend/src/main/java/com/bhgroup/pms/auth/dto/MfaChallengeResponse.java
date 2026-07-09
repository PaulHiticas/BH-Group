package com.bhgroup.pms.auth.dto;

public record MfaChallengeResponse(
        String challengeToken,
        long expiresIn
) {
}
