package dev.gymbro.gym.repository;

import java.util.List;
import java.util.Optional;

import dev.gymbro.gym.entity.WorkoutTemplate;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link WorkoutTemplate}. Every lookup is scoped by
 * {@code userId} so one user cannot address another's templates; a mismatch
 * surfaces as 404, not 403 (ADR-006).
 */
public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, Long> {

    List<WorkoutTemplate> findByUserId(Long userId);

    Optional<WorkoutTemplate> findByIdAndUserId(Long id, Long userId);
}
