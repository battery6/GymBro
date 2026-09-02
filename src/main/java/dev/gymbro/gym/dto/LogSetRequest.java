package dev.gymbro.gym.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * One performed set. {@code weightKg} may be {@code 0} (bodyweight movements are
 * logged with the effective load, ADR-011). {@code warmup} defaults to false and
 * excludes the set from volume reporting.
 */
public record LogSetRequest(
        @NotNull Long exerciseId,
        @Positive int reps,
        @NotNull @PositiveOrZero @DecimalMax("9999.99") BigDecimal weightKg,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal rpe,
        Boolean warmup) {

    public boolean warmupOrDefault() {
        return Boolean.TRUE.equals(warmup);
    }
}
