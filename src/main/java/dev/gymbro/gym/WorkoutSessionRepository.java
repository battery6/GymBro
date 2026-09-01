package dev.gymbro.gym;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    List<WorkoutSession> findByUserIdOrderByAtDateDescStartTimeDesc(Long userId);

    Optional<WorkoutSession> findByIdAndUserId(Long id, Long userId);

    List<WorkoutSession> findByUserIdAndAtDateBetweenOrderByAtDate(
            Long userId, LocalDate from, LocalDate to);
}
