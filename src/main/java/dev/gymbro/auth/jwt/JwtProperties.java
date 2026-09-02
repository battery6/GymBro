package dev.gymbro.auth.jwt;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code gymbro.jwt.*} configuration: the HS256 signing secret (must
 * be at least 32 bytes), the expected token issuer, and the access / refresh
 * token lifetimes.
 */
@ConfigurationProperties(prefix = "gymbro.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl) {
}
