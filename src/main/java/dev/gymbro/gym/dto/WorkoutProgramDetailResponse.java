package dev.gymbro.gym.dto;

import java.util.List;

import dev.gymbro.gym.entity.WorkoutProgram;

public record WorkoutProgramDetailResponse(
        Long id,
        String name,
        String description,
        List<ProgramTemplateResponse> templates) {

    public static WorkoutProgramDetailResponse from(
            WorkoutProgram program, List<ProgramTemplateResponse> templates) {
        return new WorkoutProgramDetailResponse(
                program.getId(),
                program.getName(),
                program.getDescription(),
                templates);
    }
}
