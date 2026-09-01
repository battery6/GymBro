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

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = request.email().trim();
        if (users.existsByEmail(email)) {
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
        User user = users.findByEmail(request.email().trim())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ApiException(ErrorType.INVALID_CREDENTIALS));
        return issueTokens(user);
    }

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
