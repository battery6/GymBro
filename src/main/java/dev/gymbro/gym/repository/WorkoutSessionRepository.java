package dev.gymbro.gym.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import dev.gymbro.gym.entity.WorkoutSession;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link WorkoutSession}. Every lookup is scoped by
 * {@code userId} so one user cannot address another's sessions; a mismatch
 * surfaces as 404, not 403 (ADR-006). Date filtering is on the stored
 * {@code atDate}, not a derived timestamp (ADR-002).
 */
public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    List<WorkoutSession> findByUserIdOrderByAtDateDescStartTimeDesc(Long userId);

    Optional<WorkoutSession> findByIdAndUserId(Long id, Long userId);

    List<WorkoutSession> findByUserIdAndAtDateBetweenOrderByAtDate(
            Long userId, LocalDate from, LocalDate to);
}
