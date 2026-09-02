package dev.gymbro.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. {@code timezone} is optional and, when present, must be
 * an IANA zone id (e.g. {@code Europe/Stockholm}); it is normalised server-side
 * and defaults to {@code UTC}. The timezone drives calendar-day grouping of the
 * user's logs (ADR-002).
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 64) String timezone) {
}
