package dev.gymbro.gym.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.gymbro.common.error.ApiException;
import dev.gymbro.common.error.ErrorType;
import dev.gymbro.gym.dto.AddTemplateExerciseRequest;
import dev.gymbro.gym.dto.CreateWorkoutTemplateRequest;
import dev.gymbro.gym.dto.TemplateExerciseResponse;
import dev.gymbro.gym.dto.WorkoutTemplateDetailResponse;
import dev.gymbro.gym.dto.WorkoutTemplateResponse;
import dev.gymbro.gym.entity.Exercise;
import dev.gymbro.gym.entity.TemplateExercise;
import dev.gymbro.gym.entity.WorkoutTemplate;
import dev.gymbro.gym.repository.ExerciseRepository;
import dev.gymbro.gym.repository.ProgramTemplateRepository;
import dev.gymbro.gym.repository.TemplateExerciseRepository;
import dev.gymbro.gym.repository.WorkoutTemplateRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages reusable single-workout plans and their ordered planned exercises.
 * A template is the "plan" half of the plan-vs-record split (ADR-003); all
 * operations are scoped to the owning user (ADR-006).
 */
@Service
public class WorkoutTemplateService {

    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final TemplateExerciseRepository templateExerciseRepository;
    private final ExerciseRepository exerciseRepository;
    private final ProgramTemplateRepository programTemplateRepository;

    public WorkoutTemplateService(
            WorkoutTemplateRepository workoutTemplateRepository,
            TemplateExerciseRepository templateExerciseRepository,
            ExerciseRepository exerciseRepository,
            ProgramTemplateRepository programTemplateRepository) {
        this.workoutTemplateRepository = workoutTemplateRepository;
        this.templateExerciseRepository = templateExerciseRepository;
        this.exerciseRepository = exerciseRepository;
        this.programTemplateRepository = programTemplateRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkoutTemplateResponse> list(Long userId) {
        return workoutTemplateRepository.findByUserId(userId).stream()
                .map(WorkoutTemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkoutTemplateDetailResponse get(Long userId, Long templateId) {
        WorkoutTemplate template = requireOwned(userId, templateId);

        List<TemplateExercise> planned =
                templateExerciseRepository.findByTemplateIdOrderByOrderIndex(templateId);
        Map<Long, String> namesById = exerciseNames(planned);

        List<TemplateExerciseResponse> exercises = planned.stream()
                .map(te -> TemplateExerciseResponse.from(te, namesById.get(te.getExerciseId())))
                .toList();
        return WorkoutTemplateDetailResponse.from(template, exercises);
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
    public WorkoutTemplateResponse update(
            Long userId, Long templateId, CreateWorkoutTemplateRequest request) {

        WorkoutTemplate template = requireOwned(userId, templateId);
        template.setName(request.name());
        template.setDescription(request.description());
        return WorkoutTemplateResponse.from(workoutTemplateRepository.save(template));
    }

    /**
     * Deletes a template. Refuses with {@link ErrorType#TEMPLATE_IN_USE} (409)
     * while any program still slots it, because {@code program_template} has no
     * database cascade from {@code workout_template}. Planned exercises cascade
     * away in the database, and past sessions keep their history with
     * {@code template_id} nulled.
     */
    @Transactional
    public void delete(Long userId, Long templateId) {
        WorkoutTemplate template = requireOwned(userId, templateId);

        // program_template has no cascade from workout_template — a template still
        // slotted into a program is a 409, not a raw FK failure.
        if (programTemplateRepository.existsByTemplateId(templateId)) {
            throw new ApiException(ErrorType.TEMPLATE_IN_USE);
        }

        // template_exercise rows cascade-delete in the database; a workout_session
        // that referenced this template keeps its history with template_id nulled.
        workoutTemplateRepository.delete(template);
    }

    /**
     * Appends a planned exercise as the last entry in the template. The template
     * must belong to the caller and the exercise must exist (each a 404
     * otherwise, ADR-006).
     */
    @Transactional
    public TemplateExerciseResponse addExercise(
            Long userId, Long templateId, AddTemplateExerciseRequest request) {

        // Scoped by user: another user's template (or a missing one) is a 404 (ADR-006).
        requireOwned(userId, templateId);

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

    /** Loads a template the caller owns, or throws 404 — never 403 — for anything else (ADR-006). */
    private WorkoutTemplate requireOwned(Long userId, Long templateId) {
        return workoutTemplateRepository.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ApiException(ErrorType.NOT_FOUND));
    }

    /** Resolves planned-exercise ids to names in one query, to avoid an N+1 over the entries. */
    private Map<Long, String> exerciseNames(List<TemplateExercise> planned) {
        if (planned.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = planned.stream().map(TemplateExercise::getExerciseId).toList();
        return exerciseRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Exercise::getId, Exercise::getName, (a, b) -> a));
    }
}
