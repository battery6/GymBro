package dev.gymbro.gym.dto;

import dev.gymbro.gym.entity.Exercise;

/**
 * {@code isCustom} is not stored — it is derived as {@code createdBy != null},
 * i.e. true for a user's own exercise and false for a seeded library one
 * (ADR-010).
 */
public record ExerciseResponse(
        Long id,
        String name,
        String equipment,
        String description,
        boolean isCustom) {

    public static ExerciseResponse from(Exercise exercise) {
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getEquipment(),
                exercise.getDescription(),
                exercise.isCustom());
    }
}
