package dev.gymbro.gym.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateExerciseRequest(
        @NotBlank String name,
        String equipment,
        String description) {
}
