package dev.gymbro.gym.dto;

import java.util.List;

import dev.gymbro.gym.entity.WorkoutTemplate;

public record WorkoutTemplateDetailResponse(
        Long id,
        String name,
        String description,
        List<TemplateExerciseResponse> exercises) {

    public static WorkoutTemplateDetailResponse from(
            WorkoutTemplate template, List<TemplateExerciseResponse> exercises) {
        return new WorkoutTemplateDetailResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                exercises);
    }
}
