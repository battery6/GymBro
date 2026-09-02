package dev.gymbro.gym.repository;

import java.util.List;
import java.util.Optional;

import dev.gymbro.gym.entity.SetEntry;

import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for the performed {@link SetEntry} rows hanging off a workout session (ADR-003). */
public interface SetEntryRepository extends JpaRepository<SetEntry, Long> {

    List<SetEntry> findBySessionIdOrderBySetIndex(Long sessionId);

    List<SetEntry> findBySessionIdAndExerciseIdOrderBySetIndex(Long sessionId, Long exerciseId);

    /** Scopes a set lookup to its session, so one session's request can't touch another's sets. */
    Optional<SetEntry> findByIdAndSessionId(Long id, Long sessionId);

    /** Count of sets already logged for an exercise in a session — the next {@code setIndex}. */
    int countBySessionIdAndExerciseId(Long sessionId, Long exerciseId);

    /** Whether any logged set references this exercise — blocks deleting a custom exercise. */
    boolean existsByExerciseId(Long exerciseId);

    void deleteBySessionId(Long sessionId);
}
