package dev.gymbro.gym.dto;

import java.math.BigDecimal;

import dev.gymbro.gym.entity.TemplateExercise;

public record TemplateExerciseResponse(
        Long id,
        Long exerciseId,
        String exerciseName,
        int orderIndex,
        int targetSets,
        int targetReps,
        BigDecimal targetRpe) {

    public static TemplateExerciseResponse from(TemplateExercise templateExercise, String exerciseName) {
        return new TemplateExerciseResponse(
                templateExercise.getId(),
                templateExercise.getExerciseId(),
                exerciseName,
                templateExercise.getOrderIndex(),
                templateExercise.getTargetSets(),
                templateExercise.getTargetReps(),
                templateExercise.getTargetRpe());
    }
}
