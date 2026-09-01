package dev.gymbro.gym.repository;

import java.util.List;

import dev.gymbro.gym.entity.ExerciseMuscleGroup;
import dev.gymbro.gym.entity.ExerciseMuscleGroupId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseMuscleGroupRepository
        extends JpaRepository<ExerciseMuscleGroup, ExerciseMuscleGroupId> {

    List<ExerciseMuscleGroup> findByExerciseId(Long exerciseId);

    List<ExerciseMuscleGroup> findByMuscleGroupId(Long muscleGroupId);

    void deleteByExerciseId(Long exerciseId);
}
