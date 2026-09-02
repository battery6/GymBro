package dev.gymbro.gym.service;

import java.util.Comparator;
import java.util.List;

import dev.gymbro.common.error.ApiException;
import dev.gymbro.common.error.ErrorType;
import dev.gymbro.gym.dto.CreateExerciseRequest;
import dev.gymbro.gym.dto.ExerciseResponse;
import dev.gymbro.gym.entity.Exercise;
import dev.gymbro.gym.repository.ExerciseRepository;
import dev.gymbro.gym.repository.SetEntryRepository;
import dev.gymbro.gym.repository.TemplateExerciseRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The exercise catalogue: the shared seeded library plus each user's private
 * custom movements. Enforces that a user only sees and mutates the library and
 * their own additions.
 */
@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final TemplateExerciseRepository templateExerciseRepository;
    private final SetEntryRepository setEntryRepository;

    public ExerciseService(
            ExerciseRepository exerciseRepository,
            TemplateExerciseRepository templateExerciseRepository,
            SetEntryRepository setEntryRepository) {
        this.exerciseRepository = exerciseRepository;
        this.templateExerciseRepository = templateExerciseRepository;
        this.setEntryRepository = setEntryRepository;
    }

    /** The system library plus the caller's own custom exercises, ordered by name. */
    @Transactional(readOnly = true)
    public List<ExerciseResponse> list(Long userId) {
        return exerciseRepository.findByCreatedByIsNullOrCreatedBy(userId).stream()
                .sorted(Comparator.comparing(Exercise::getName, String.CASE_INSENSITIVE_ORDER))
                .map(ExerciseResponse::from)
                .toList();
    }

    @Transactional
    public ExerciseResponse create(Long userId, CreateExerciseRequest request) {
        Exercise exercise = new Exercise();
        exercise.setCreatedBy(userId);
        exercise.setName(request.name());
        exercise.setEquipment(request.equipment());
        exercise.setDescription(request.description());
        return ExerciseResponse.from(exerciseRepository.save(exercise));
    }

    /**
     * Deletes one of the caller's custom exercises. A library exercise (or one
     * owned by another user, or a missing one) is a 404 (ADR-006); a custom
     * exercise still referenced by a template or a logged set is a 409 (ADR-006).
     */
    @Transactional
    public void delete(Long userId, Long exerciseId) {
        Exercise exercise = exerciseRepository.findByIdAndCreatedBy(exerciseId, userId)
                .orElseThrow(() -> new ApiException(ErrorType.NOT_FOUND));

        if (templateExerciseRepository.existsByExerciseId(exerciseId)
                || setEntryRepository.existsByExerciseId(exerciseId)) {
            throw new ApiException(ErrorType.EXERCISE_IN_USE);
        }
        exerciseRepository.delete(exercise);
    }
}
