package dev.gymbro.auth.service;

import java.time.DateTimeException;
import java.time.ZoneId;

import dev.gymbro.auth.dto.LoginRequest;
import dev.gymbro.auth.dto.RefreshRequest;
import dev.gymbro.auth.dto.RegisterRequest;
import dev.gymbro.auth.dto.TokenResponse;
import dev.gymbro.auth.jwt.JwtService;
import dev.gymbro.auth.refresh.RefreshTokenService;
import dev.gymbro.common.error.ApiException;
import dev.gymbro.common.error.ErrorType;
import dev.gymbro.user.entity.User;
import dev.gymbro.user.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the credential lifecycle: verifies passwords, creates users, and
 * issues a JWT access token paired with a rotating refresh token on every
 * successful register / login / refresh.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;

    public AuthService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
    }

    /**
     * Creates a user and logs them in. Rejects an already-registered email with
     * {@link ErrorType#EMAIL_ALREADY_USED} (409) and an unparseable timezone
     * with {@link ErrorType#INVALID_TIMEZONE}.
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = request.email().trim();
        if (users.existsByEmail(email)) {
            // Registration unavoidably reveals that an email is taken; login and
            // refresh do not leak existence (ADR-017).
            throw new ApiException(ErrorType.EMAIL_ALREADY_USED);
        }
        String timezone = normalizeTimezone(request.timezone());

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setTimezone(timezone);
        users.save(user);

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        // Unknown email and wrong password collapse into the same failure so an
        // attacker can't enumerate which addresses have accounts (ADR-017).
        User user = users.findByEmail(request.email().trim())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ApiException(ErrorType.INVALID_CREDENTIALS));
        return issueTokens(user);
    }

    /**
     * Rotates the presented refresh token and issues a fresh token pair. A token
     * whose user has since been deleted also fails as
     * {@link ErrorType#INVALID_REFRESH_TOKEN} — the outcome a client should treat
     * identically (ADR-017).
     */
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        Long userId = refreshTokens.rotate(request.refreshToken());
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorType.INVALID_REFRESH_TOKEN));
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokens.revoke(request.refreshToken());
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokens.issue(user.getId());
        return TokenResponse.bearer(accessToken, refreshToken, jwtService.accessTtlSeconds());
    }

    /** Blank / absent timezone defaults to {@code UTC}; anything else must be a resolvable zone id. */
    private static String normalizeTimezone(String requested) {
        if (requested == null || requested.isBlank()) {
            return "UTC";
        }
        try {
            return ZoneId.of(requested.trim()).getId();
        } catch (DateTimeException e) {
            throw new ApiException(ErrorType.INVALID_TIMEZONE, "Unknown timezone identifier: " + requested);
        }
    }
}
