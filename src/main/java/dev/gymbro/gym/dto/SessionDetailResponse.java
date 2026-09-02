package dev.gymbro.gym.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import dev.gymbro.gym.entity.WorkoutSession;

public record SessionDetailResponse(
        Long id,
        Long templateId,
        LocalDate atDate,
        Instant startTime,
        Instant endTime,
        boolean complete,
        String notes,
        List<SetEntryResponse> sets) {

    public static SessionDetailResponse from(WorkoutSession session, List<SetEntryResponse> sets) {
        return new SessionDetailResponse(
                session.getId(),
                session.getTemplateId(),
                session.getAtDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.isComplete(),
                session.getNotes(),
                sets);
    }
}
