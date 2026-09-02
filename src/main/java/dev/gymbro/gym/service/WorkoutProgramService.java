package dev.gymbro.gym.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.gymbro.common.error.ApiException;
import dev.gymbro.common.error.ErrorType;
import dev.gymbro.gym.dto.AddProgramTemplateRequest;
import dev.gymbro.gym.dto.CreateWorkoutProgramRequest;
import dev.gymbro.gym.dto.ProgramTemplateResponse;
import dev.gymbro.gym.dto.WorkoutProgramDetailResponse;
import dev.gymbro.gym.dto.WorkoutProgramResponse;
import dev.gymbro.gym.entity.ProgramTemplate;
import dev.gymbro.gym.entity.WorkoutProgram;
import dev.gymbro.gym.entity.WorkoutTemplate;
import dev.gymbro.gym.repository.ProgramTemplateRepository;
import dev.gymbro.gym.repository.WorkoutProgramRepository;
import dev.gymbro.gym.repository.WorkoutTemplateRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutProgramService {

    private final WorkoutProgramRepository workoutProgramRepository;
    private final ProgramTemplateRepository programTemplateRepository;
    private final WorkoutTemplateRepository workoutTemplateRepository;

    public WorkoutProgramService(
            WorkoutProgramRepository workoutProgramRepository,
            ProgramTemplateRepository programTemplateRepository,
            WorkoutTemplateRepository workoutTemplateRepository) {
        this.workoutProgramRepository = workoutProgramRepository;
        this.programTemplateRepository = programTemplateRepository;
        this.workoutTemplateRepository = workoutTemplateRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkoutProgramResponse> list(Long userId) {
        return workoutProgramRepository.findByUserId(userId).stream()
                .map(WorkoutProgramResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkoutProgramDetailResponse get(Long userId, Long programId) {
        WorkoutProgram program = requireOwned(userId, programId);

        List<ProgramTemplate> slots =
                programTemplateRepository.findByProgramIdOrderByOrderIndex(programId);
        Map<Long, String> namesById = templateNames(slots);

        List<ProgramTemplateResponse> templates = slots.stream()
                .map(slot -> ProgramTemplateResponse.from(slot, namesById.get(slot.getTemplateId())))
                .toList();
        return WorkoutProgramDetailResponse.from(program, templates);
    }

    @Transactional
    public WorkoutProgramResponse create(Long userId, CreateWorkoutProgramRequest request) {
        WorkoutProgram program = new WorkoutProgram();
        program.setUserId(userId);
        program.setName(request.name());
        program.setDescription(request.description());

        return WorkoutProgramResponse.from(workoutProgramRepository.save(program));
    }

    @Transactional
    public WorkoutProgramResponse update(
            Long userId, Long programId, CreateWorkoutProgramRequest request) {

        WorkoutProgram program = requireOwned(userId, programId);
        program.setName(request.name());
        program.setDescription(request.description());
        return WorkoutProgramResponse.from(workoutProgramRepository.save(program));
    }

    @Transactional
    public void delete(Long userId, Long programId) {
        WorkoutProgram program = requireOwned(userId, programId);
        // program_template rows cascade-delete in the database; the referenced
        // workout_template rows are untouched.
        workoutProgramRepository.delete(program);
    }

    @Transactional
    public ProgramTemplateResponse addTemplate(
            Long userId, Long programId, AddProgramTemplateRequest request) {

        // Scoped by user: another user's program (or a missing one) is a 404 (ADR-006).
        requireOwned(userId, programId);

        // The template must also belong to the caller, else 404 (existence not leaked).
        WorkoutTemplate template = workoutTemplateRepository
                .findByIdAndUserId(request.templateId(), userId)
                .orElseThrow(() -> new ApiException(ErrorType.NOT_FOUND));

        int nextOrderIndex = programTemplateRepository
                .findFirstByProgramIdOrderByOrderIndexDesc(programId)
                .map(existing -> existing.getOrderIndex() + 1)
                .orElse(0);

        ProgramTemplate slot = new ProgramTemplate();
        slot.setProgramId(programId);
        slot.setTemplateId(request.templateId());
        slot.setOrderIndex(nextOrderIndex);

        ProgramTemplate saved = programTemplateRepository.save(slot);
        return ProgramTemplateResponse.from(saved, template.getName());
    }

    private WorkoutProgram requireOwned(Long userId, Long programId) {
        return workoutProgramRepository.findByIdAndUserId(programId, userId)
                .orElseThrow(() -> new ApiException(ErrorType.NOT_FOUND));
    }

    private Map<Long, String> templateNames(List<ProgramTemplate> slots) {
        if (slots.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = slots.stream().map(ProgramTemplate::getTemplateId).toList();
        return workoutTemplateRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(WorkoutTemplate::getId, WorkoutTemplate::getName, (a, b) -> a));
    }
}
