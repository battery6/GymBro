package dev.gymbro.gym.dto;

import java.time.Instant;
import java.time.LocalDate;

import dev.gymbro.gym.entity.WorkoutSession;

public record SessionResponse(
        Long id,
        Long templateId,
        LocalDate atDate,
        Instant startTime,
        Instant endTime,
        boolean complete,
        String notes) {

    public static SessionResponse from(WorkoutSession session) {
        return new SessionResponse(
                session.getId(),
                session.getTemplateId(),
                session.getAtDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.isComplete(),
                session.getNotes());
    }
}
