package com.bhgroup.pms.security;

import com.bhgroup.pms.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_MFA_CHALLENGE = "MFA_CHALLENGE";

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final long MFA_CHALLENGE_EXPIRATION_MS = 5 * 60 * 1000L;

    private final AppProperties appProperties;

    private Key signingKey() {
        return Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email, String role) {
        return buildToken(userId.toString(), TOKEN_TYPE_ACCESS,
                appProperties.getJwt().getAccessTokenExpirationMs(),
                claims -> claims.claim(CLAIM_EMAIL, email).claim(CLAIM_ROLE, role));
    }

    public String generateMfaChallengeToken(UUID userId) {
        return buildToken(userId.toString(), TOKEN_TYPE_MFA_CHALLENGE, MFA_CHALLENGE_EXPIRATION_MS, claims -> claims);
    }

    private String buildToken(String subject, String type, long expirationMs,
                               java.util.function.UnaryOperator<io.jsonwebtoken.JwtBuilder> customizer) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuer(appProperties.getJwt().getIssuer())
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));
        return customizer.apply(builder)
                .signWith(signingKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValidOfType(String token, String expectedType) {
        try {
            Claims claims = parseClaims(token);
            return expectedType.equals(claims.get(CLAIM_TYPE, String.class));
        } catch (ExpiredJwtException ex) {
            log.debug("JWT expired: {}", ex.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT invalid: {}", ex.getMessage());
            return false;
        }
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String getEmail(String token) {
        return parseClaims(token).get(CLAIM_EMAIL, String.class);
    }

    public long getAccessTokenExpirationMs() {
        return appProperties.getJwt().getAccessTokenExpirationMs();
    }

    public long getRefreshTokenExpirationMs() {
        return appProperties.getJwt().getRefreshTokenExpirationMs();
    }
}
