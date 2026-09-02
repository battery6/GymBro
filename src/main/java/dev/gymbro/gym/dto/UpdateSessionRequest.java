package dev.gymbro.gym.dto;

import java.time.Instant;

/**
 * Partial update of a session. A non-null field is applied; a null field is left
 * unchanged. Setting {@code endTime} marks the session complete (ADR, DESIGN 3.1).
 */
public record UpdateSessionRequest(
        Instant endTime,
        String notes) {
}
