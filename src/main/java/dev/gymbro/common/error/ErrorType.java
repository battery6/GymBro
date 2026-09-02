package dev.gymbro.common.error;

import org.springframework.http.HttpStatus;

/**
 * Enumerates the application's expected failure modes. Each value carries the
 * HTTP status and a human-readable title; {@link #code()} is the stable machine
 * identifier surfaced in the {@code code} property of the Problem Details body.
 */
public enum ErrorType {

    EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "Email already registered"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is invalid, expired, or already used"),
    INVALID_TIMEZONE(HttpStatus.BAD_REQUEST, "Unknown timezone identifier"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");

    private final HttpStatus status;
    private final String title;

    ErrorType(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    public String code() {
        return name();
    }
}
