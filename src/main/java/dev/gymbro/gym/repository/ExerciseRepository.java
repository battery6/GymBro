package dev.gymbro.gym.repository;

import java.util.List;
import java.util.Optional;

import dev.gymbro.gym.entity.Exercise;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link Exercise}. A {@code null} {@code createdBy} is a
 * system-seeded library movement; a non-null value is a user's custom one
 * (ADR-010), and queries here distinguish the two.
 */
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    /** System-seeded library exercises. */
    List<Exercise> findByCreatedByIsNull();

    /** Exercises visible to a user: the system library plus their own custom ones. */
    List<Exercise> findByCreatedByIsNullOrCreatedBy(Long createdBy);

    /** A custom exercise the given user owns; used to authorise edits/deletes. */
    Optional<Exercise> findByIdAndCreatedBy(Long id, Long createdBy);

    /** Guards against a custom exercise shadowing a library name (case-insensitive). */
    boolean existsByNameIgnoreCaseAndCreatedByIsNull(String name);
}
