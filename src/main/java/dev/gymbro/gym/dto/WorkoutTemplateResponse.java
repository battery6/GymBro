package dev.gymbro.gym.dto;

import dev.gymbro.gym.entity.WorkoutTemplate;

public record WorkoutTemplateResponse(
        Long id,
        String name,
        String description) {

    public static WorkoutTemplateResponse from(WorkoutTemplate workoutTemplate) {
        return new WorkoutTemplateResponse(
                workoutTemplate.getId(),
                workoutTemplate.getName(),
                workoutTemplate.getDescription());
    }
}
