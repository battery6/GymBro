package dev.gymbro.gym.dto;

public record WorkoutTemplateResponse(
    Long id,
    String name,
    String description) {

        public static WorkoutTemplateResponse fromEntity(dev.gymbro.gym.entity.WorkoutTemplate workoutTemplate) {
            return new WorkoutTemplateResponse(
                workoutTemplate.getId(),
                workoutTemplate.getName(),
                workoutTemplate.getDescription()
            );
        }
}