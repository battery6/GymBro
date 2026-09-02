package dev.gymbro.gym.dto;

import dev.gymbro.gym.entity.Exercise;

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
