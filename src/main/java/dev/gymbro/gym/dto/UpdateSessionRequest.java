package dev.gymbro.gym.dto;

import java.time.Instant;

/**
 * Partial update of a session. A non-null field is applied; a null field is left
 * unchanged (so a field cannot be cleared through this endpoint). Setting
 * {@code endTime} is what marks the session complete (DESIGN &sect;3.1).
 */
public record UpdateSessionRequest(
        Instant endTime,
        String notes) {
}
