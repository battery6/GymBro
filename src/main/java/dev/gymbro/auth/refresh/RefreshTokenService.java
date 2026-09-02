package dev.gymbro.auth.refresh;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import dev.gymbro.auth.jwt.JwtProperties;
import dev.gymbro.common.error.ApiException;
import dev.gymbro.common.error.ErrorType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages opaque refresh tokens. The raw token is returned to the client once;
 * only its SHA-256 hash is persisted. Tokens are single-use: {@link #rotate}
 * revokes the presented token and the caller issues a fresh one.
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties jwtProperties) {
        this.repository = repository;
        this.ttl = jwtProperties.refreshTokenTtl();
    }

    /**
     * Generates a new refresh token, persists only its hash, and returns the raw
     * token — the one and only time the caller can see the cleartext value.
     */
    @Transactional
    public String issue(Long userId) {
        byte[] raw = new byte[TOKEN_BYTES];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        RefreshToken entity = new RefreshToken();
        entity.setUserId(userId);
        entity.setTokenHash(hash(token));
        entity.setExpiresAt(Instant.now().plus(ttl));
        repository.save(entity);
        return token;
    }

    /**
     * Validates the presented token, revokes it (tokens are single-use), and
     * returns the owning user id. Throws {@link ApiException} with
     * {@link ErrorType#INVALID_REFRESH_TOKEN} if the token is unknown, expired,
     * or already revoked.
     */
    @Transactional
    public Long rotate(String presentedToken) {
        RefreshToken entity = repository.findByTokenHash(hash(presentedToken))
                .orElseThrow(() -> new ApiException(ErrorType.INVALID_REFRESH_TOKEN));
        if (!entity.isActive()) {
            throw new ApiException(ErrorType.INVALID_REFRESH_TOKEN);
        }
        entity.setRevokedAt(Instant.now());
        return entity.getUserId();
    }

    /**
     * Best-effort revocation used by logout: an unknown or already-inactive
     * token is silently ignored so the endpoint is idempotent and reveals
     * nothing about which tokens exist.
     */
    @Transactional
    public void revoke(String presentedToken) {
        repository.findByTokenHash(hash(presentedToken))
                .filter(RefreshToken::isActive)
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
