package dev.gymbro.gym;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, Long> {

    List<WorkoutTemplate> findByUserId(Long userId);

    Optional<WorkoutTemplate> findByIdAndUserId(Long id, Long userId);
}
