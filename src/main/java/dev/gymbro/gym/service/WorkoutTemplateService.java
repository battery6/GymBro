package dev.gymbro.gym.service;

import dev.gymbro.common.error.ApiException;
import dev.gymbro.common.error.ErrorType;
import dev.gymbro.gym.dto.AddTemplateExerciseRequest;
import dev.gymbro.gym.dto.CreateWorkoutTemplateRequest;
import dev.gymbro.gym.dto.TemplateExerciseResponse;
import dev.gymbro.gym.dto.WorkoutTemplateResponse;
import dev.gymbro.gym.entity.Exercise;
import dev.gymbro.gym.entity.TemplateExercise;
import dev.gymbro.gym.entity.WorkoutTemplate;
import dev.gymbro.gym.repository.ExerciseRepository;
import dev.gymbro.gym.repository.TemplateExerciseRepository;
import dev.gymbro.gym.repository.WorkoutTemplateRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutTemplateService {

    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final TemplateExerciseRepository templateExerciseRepository;
    private final ExerciseRepository exerciseRepository;

    public WorkoutTemplateService(
            WorkoutTemplateRepository workoutTemplateRepository,
            TemplateExerciseRepository templateExerciseRepository,
            ExerciseRepository exerciseRepository) {
        this.workoutTemplateRepository = workoutTemplateRepository;
        this.templateExerciseRepository = templateExerciseRepository;
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional
    public WorkoutTemplateResponse create(Long userId, CreateWorkoutTemplateRequest request) {
        WorkoutTemplate template = new WorkoutTemplate();
        template.setUserId(userId);
        template.setName(request.name());
        template.setDescription(request.description());

        return WorkoutTemplateResponse.from(workoutTemplateRepository.save(template));
    }

    @Transactional
    public TemplateExerciseResponse addExercise(
            Long userId, Long templateId, AddTemplateExerciseRequest request) {

        // Scoped by user: another user's template (or a missing one) is a 404 (ADR-006).
        workoutTemplateRepository.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ApiException(ErrorType.NOT_FOUND));

        Exercise exercise = exerciseRepository.findById(request.exerciseId())
                .orElseThrow(() -> new ApiException(ErrorType.NOT_FOUND));

        int nextOrderIndex = templateExerciseRepository
                .findFirstByTemplateIdOrderByOrderIndexDesc(templateId)
                .map(existing -> existing.getOrderIndex() + 1)
                .orElse(0);

        TemplateExercise templateExercise = new TemplateExercise();
        templateExercise.setTemplateId(templateId);
        templateExercise.setExerciseId(request.exerciseId());
        templateExercise.setOrderIndex(nextOrderIndex);
        templateExercise.setTargetSets(request.targetSets());
        templateExercise.setTargetReps(request.targetReps());
        templateExercise.setTargetRpe(request.targetRpe());

        TemplateExercise saved = templateExerciseRepository.save(templateExercise);
        return TemplateExerciseResponse.from(saved, exercise.getName());
    }
}
