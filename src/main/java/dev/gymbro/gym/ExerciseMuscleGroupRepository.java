package dev.gymbro.gym;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseMuscleGroupRepository
        extends JpaRepository<ExerciseMuscleGroup, ExerciseMuscleGroupId> {

    List<ExerciseMuscleGroup> findByExerciseId(Long exerciseId);

    List<ExerciseMuscleGroup> findByMuscleGroupId(Long muscleGroupId);

    void deleteByExerciseId(Long exerciseId);
}
