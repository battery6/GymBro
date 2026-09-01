package dev.gymbro.gym.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkoutTemplateRequest(
    @NotBlank String name,
    String description) {
}