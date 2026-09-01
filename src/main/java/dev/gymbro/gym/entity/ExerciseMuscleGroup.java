package dev.gymbro.gym.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Links an {@link Exercise} to a {@link MuscleGroup} it trains. {@code isPrimary}
 * marks the main mover(s); the volume-by-muscle report can filter on it to avoid
 * multi-counting compound lifts (ADR-001).
 */
@Entity
@Table(name = "exercise_muscle_group")
@IdClass(ExerciseMuscleGroupId.class)
public class ExerciseMuscleGroup {

    @Id
    @Column(name = "exercise_id")
    private Long exerciseId;

    @Id
    @Column(name = "muscle_group_id")
    private Long muscleGroupId;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    public ExerciseMuscleGroup() {
    }

    public ExerciseMuscleGroup(Long exerciseId, Long muscleGroupId, boolean primary) {
        this.exerciseId = exerciseId;
        this.muscleGroupId = muscleGroupId;
        this.primary = primary;
    }

    public Long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(Long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public Long getMuscleGroupId() {
        return muscleGroupId;
    }

    public void setMuscleGroupId(Long muscleGroupId) {
        this.muscleGroupId = muscleGroupId;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
