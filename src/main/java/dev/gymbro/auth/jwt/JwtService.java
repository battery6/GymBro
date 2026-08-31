package dev.gymbro.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import dev.gymbro.auth.AuthUser;
import dev.gymbro.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

/** Issues and verifies stateless HS256 access tokens. */
@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTtl;

    public JwtService(JwtProperties props) {
        byte[] secret = props.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "gymbro.jwt.secret must be at least 32 bytes for HS256 (got " + secret.length + ")");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.issuer = props.issuer();
        this.accessTtl = props.accessTokenTtl();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    /**
     * Parses and verifies an access token.
     *
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public AuthUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthUser(Long.valueOf(claims.getSubject()), claims.get("email", String.class));
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }
}
