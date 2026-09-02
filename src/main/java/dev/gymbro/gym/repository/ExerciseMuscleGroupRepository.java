package dev.gymbro.gym.repository;

import java.util.List;

import dev.gymbro.gym.entity.ExerciseMuscleGroup;
import dev.gymbro.gym.entity.ExerciseMuscleGroupId;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for the exercise&harr;muscle-group join. Queried from both sides:
 * by exercise (what a movement trains) and by muscle group (which movements feed
 * the volume-by-muscle report, ADR-001).
 */
public interface ExerciseMuscleGroupRepository
        extends JpaRepository<ExerciseMuscleGroup, ExerciseMuscleGroupId> {

    List<ExerciseMuscleGroup> findByExerciseId(Long exerciseId);

    List<ExerciseMuscleGroup> findByMuscleGroupId(Long muscleGroupId);

    void deleteByExerciseId(Long exerciseId);
}
