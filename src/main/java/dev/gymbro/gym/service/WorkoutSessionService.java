package dev.gymbro.gym.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.gymbro.common.error.ApiException;
import dev.gymbro.common.error.ErrorType;
import dev.gymbro.gym.dto.CreateSessionRequest;
import dev.gymbro.gym.dto.LogSetRequest;
import dev.gymbro.gym.dto.SessionDetailResponse;
import dev.gymbro.gym.dto.SessionResponse;
import dev.gymbro.gym.dto.SetEntryResponse;
import dev.gymbro.gym.dto.UpdateSessionRequest;
import dev.gymbro.gym.entity.SetEntry;
import dev.gymbro.gym.entity.WorkoutSession;
import dev.gymbro.gym.repository.ExerciseRepository;
import dev.gymbro.gym.repository.SetEntryRepository;
import dev.gymbro.gym.repository.WorkoutSessionRepository;
import dev.gymbro.gym.repository.WorkoutTemplateRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutSessionService {

    private final WorkoutSessionRepository workoutSessionRepository;
    private final SetEntryRepository setEntryRepository;
    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final ExerciseRepository exerciseRepository;

    public WorkoutSessionService(
            WorkoutSessionRepository workoutSessionRepository,
            SetEntryRepository setEntryRepository,
            WorkoutTemplateRepository workoutTemplateRepository,
            ExerciseRepository exerciseRepository) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.setEntryRepository = setEntryRepository;
        this.workoutTemplateRepository = workoutTemplateRepository;
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional
    public SessionResponse create(Long userId, CreateSessionRequest request) {
        WorkoutSession session = new WorkoutSession();
        session.setUserId(userId);
        session.setAtDate(request.atDate() != null ? request.atDate() : LocalDate.now());
        session.setStartTime(Instant.now());
        session.setNotes(request.notes());

        if (request.templateId() != null) {
            // A template the caller doesn't own (or a missing one) is a 404 (ADR-006).
            workoutTemplateRepository.findByIdAndUserId(request.templateId(), userId)
                    .orElseThrow(() -> new ApiException(ErrorType.NOT_FOUND));
            session.setTemplateId(request.templateId());
        }

        return SessionResponse.from(workoutSessionRepository.save(session));
    }

    public List<SessionResponse> list(Long userId, LocalDate from, LocalDate to) {
        List<WorkoutSession> sessions = (from != null && to != null)
                ? workoutSessionRepository.findByUserIdAndAtDateBetweenOrderByAtDate(userId, from, to)
                : workoutSessionRepository.findByUserIdOrderByAtDateDescStartTimeDesc(userId);
        return sessions.stream().map(SessionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SessionDetailResponse get(Long userId, Long sessionId) {
        WorkoutSession session = requireOwned(userId, sessionId);
        List<SetEntryResponse> sets = setEntryRepository.findBySessionIdOrderBySetIndex(sessionId)
                .stream().map(SetEntryResponse::from).toList();
        return SessionDetailResponse.from(session, sets);
    }

    @Transactional
    public SessionResponse update(Long userId, Long sessionId, UpdateSessionRequest request) {
        WorkoutSession session = requireOwned(userId, sessionId);
        if (request.endTime() != null) {
            session.setEndTime(request.endTime());
        }
        if (request.notes() != null) {
            session.setNotes(request.notes());
        }
        return SessionResponse.from(workoutSessionRepository.save(session));
    }

    @Transactional
    public void delete(Long userId, Long sessionId) {
        WorkoutSession session = requireOwned(userId, sessionId);
        // set_entry rows cascade-delete in the database.
        workoutSessionRepository.delete(session);
    }

    /**
     * Appends one or more sets to a session. {@code setIndex} is assigned per
     * exercise: sets of a given exercise are numbered 0, 1, 2, ... in the order
     * they are logged.
     */
    @Transactional
    public List<SetEntryResponse> addSets(Long userId, Long sessionId, List<LogSetRequest> requests) {
        requireOwned(userId, sessionId);

        Map<Long, Integer> nextIndexByExercise = new HashMap<>();
        List<SetEntry> toSave = new ArrayList<>(requests.size());

        for (LogSetRequest request : requests) {
            if (!exerciseRepository.existsById(request.exerciseId())) {
                throw new ApiException(ErrorType.NOT_FOUND);
            }
            int setIndex = nextIndexByExercise.computeIfAbsent(
                    request.exerciseId(),
                    id -> setEntryRepository.countBySessionIdAndExerciseId(sessionId, id));
            nextIndexByExercise.put(request.exerciseId(), setIndex + 1);

            SetEntry set = new SetEntry();
            set.setSessionId(sessionId);
            set.setExerciseId(request.exerciseId());
            set.setSetIndex(setIndex);
            set.setReps(request.reps());
            set.setWeightKg(request.weightKg());
            set.setRpe(request.rpe());
            set.setWarmup(request.warmupOrDefault());
            toSave.add(set);
        }

        return setEntryRepository.saveAll(toSave).stream().map(SetEntryResponse::from).toList();
    }

    @Transactional
    public void deleteSet(Long userId, Long sessionId, Long setId) {
        requireOwned(userId, sessionId);
        SetEntry set = setEntryRepository.findByIdAndSessionId(setId, sessionId)
                .orElseThrow(() -> new ApiException(ErrorType.NOT_FOUND));
        setEntryRepository.delete(set);
    }

    private WorkoutSession requireOwned(Long userId, Long sessionId) {
        return workoutSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ApiException(ErrorType.NOT_FOUND));
    }
}
