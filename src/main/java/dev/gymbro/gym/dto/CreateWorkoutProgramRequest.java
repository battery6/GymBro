package dev.gymbro.gym.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkoutProgramRequest(
        @NotBlank String name,
        String description) {
}
