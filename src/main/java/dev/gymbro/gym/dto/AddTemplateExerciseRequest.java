package dev.gymbro.gym.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * A planned exercise to append to a template. {@code targetReps} is a single
 * integer, not a range (DESIGN &sect;3.1). {@code targetRpe} is optional and, if
 * given, is a rate-of-perceived-exertion target on a 0&ndash;10 scale.
 */
public record AddTemplateExerciseRequest(
        @NotNull Long exerciseId,
        @Positive int targetSets,
        @Positive int targetReps,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal targetRpe) {
}
