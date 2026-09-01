package dev.gymbro.gym.repository;

import java.util.List;
import java.util.Optional;

import dev.gymbro.gym.entity.Exercise;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    /** System-seeded library exercises. */
    List<Exercise> findByCreatedByIsNull();

    /** Exercises visible to a user: the system library plus their own custom ones. */
    List<Exercise> findByCreatedByIsNullOrCreatedBy(Long createdBy);

    Optional<Exercise> findByIdAndCreatedBy(Long id, Long createdBy);

    boolean existsByNameIgnoreCaseAndCreatedByIsNull(String name);
}
