package dev.gymbro.auth.dto;

/**
 * A newly issued credential pair. {@code tokenType} is always {@code "Bearer"};
 * {@code expiresInSeconds} is the lifetime of {@code accessToken} only — the
 * refresh token lives longer and is rotated on each use.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds) {

    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
