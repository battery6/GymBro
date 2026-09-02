package dev.gymbro.gym.dto;

import java.time.LocalDate;

/**
 * Starts a workout session. {@code templateId} is optional (freeform sessions
 * are allowed). {@code atDate} is the calendar day the session belongs to, set
 * from the client's local day (ADR-002); when omitted the server's current date
 * is used.
 */
public record CreateSessionRequest(
        Long templateId,
        LocalDate atDate,
        String notes) {
}
