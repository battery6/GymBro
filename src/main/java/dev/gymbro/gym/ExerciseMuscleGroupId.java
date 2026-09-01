package dev.gymbro.gym;

import java.io.Serializable;
import java.util.Objects;

/** Composite primary key for {@link ExerciseMuscleGroup}. */
public class ExerciseMuscleGroupId implements Serializable {

    private Long exerciseId;
    private Long muscleGroupId;

    public ExerciseMuscleGroupId() {
    }

    public ExerciseMuscleGroupId(Long exerciseId, Long muscleGroupId) {
        this.exerciseId = exerciseId;
        this.muscleGroupId = muscleGroupId;
    }

    public Long getExerciseId() {
        return exerciseId;
    }

    public Long getMuscleGroupId() {
        return muscleGroupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExerciseMuscleGroupId other)) {
            return false;
        }
        return Objects.equals(exerciseId, other.exerciseId)
                && Objects.equals(muscleGroupId, other.muscleGroupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exerciseId, muscleGroupId);
    }
}
