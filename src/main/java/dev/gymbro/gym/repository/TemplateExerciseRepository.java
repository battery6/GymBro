package dev.gymbro.gym.repository;

import java.util.List;
import java.util.Optional;

import dev.gymbro.gym.entity.TemplateExercise;

import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for the ordered planned exercises within a {@link dev.gymbro.gym.entity.WorkoutTemplate}. */
public interface TemplateExerciseRepository extends JpaRepository<TemplateExercise, Long> {

    List<TemplateExercise> findByTemplateIdOrderByOrderIndex(Long templateId);

    List<TemplateExercise> findByExerciseId(Long exerciseId);

    /** Whether any template still plans this exercise — blocks deleting a custom exercise. */
    boolean existsByExerciseId(Long exerciseId);

    /** The current last entry, so a new one can be appended at {@code orderIndex + 1}. */
    Optional<TemplateExercise> findFirstByTemplateIdOrderByOrderIndexDesc(Long templateId);

    void deleteByTemplateId(Long templateId);
}
