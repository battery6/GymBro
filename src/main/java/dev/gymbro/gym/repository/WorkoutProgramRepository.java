package dev.gymbro.gym.repository;

import java.util.List;
import java.util.Optional;

import dev.gymbro.gym.entity.WorkoutProgram;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link WorkoutProgram}. Every lookup is scoped by
 * {@code userId} so one user cannot address another's programs; a mismatch
 * surfaces as 404, not 403 (ADR-006).
 */
public interface WorkoutProgramRepository extends JpaRepository<WorkoutProgram, Long> {

    List<WorkoutProgram> findByUserId(Long userId);

    Optional<WorkoutProgram> findByIdAndUserId(Long id, Long userId);
}
