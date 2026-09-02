package dev.gymbro.auth.refresh;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link RefreshToken}. Tokens are located by their stored
 * SHA-256 hash, since the raw value is never persisted.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
