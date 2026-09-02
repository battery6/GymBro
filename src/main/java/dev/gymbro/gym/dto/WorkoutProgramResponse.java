package dev.gymbro.gym.dto;

import dev.gymbro.gym.entity.WorkoutProgram;

public record WorkoutProgramResponse(
        Long id,
        String name,
        String description) {

    public static WorkoutProgramResponse from(WorkoutProgram program) {
        return new WorkoutProgramResponse(
                program.getId(),
                program.getName(),
                program.getDescription());
    }
}
