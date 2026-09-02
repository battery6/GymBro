package dev.gymbro.gym.dto;

import java.math.BigDecimal;

import dev.gymbro.gym.entity.SetEntry;

public record SetEntryResponse(
        Long id,
        Long exerciseId,
        int setIndex,
        int reps,
        BigDecimal weightKg,
        BigDecimal rpe,
        boolean warmup) {

    public static SetEntryResponse from(SetEntry setEntry) {
        return new SetEntryResponse(
                setEntry.getId(),
                setEntry.getExerciseId(),
                setEntry.getSetIndex(),
                setEntry.getReps(),
                setEntry.getWeightKg(),
                setEntry.getRpe(),
                setEntry.isWarmup());
    }
}
