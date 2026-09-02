package dev.gymbro.gym.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddTemplateExerciseRequest(
        @NotNull Long exerciseId,
        @Positive int targetSets,
        @Positive int targetReps,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal targetRpe) {
}
