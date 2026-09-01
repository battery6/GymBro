package dev.gymbro.gym;

import java.math.BigDecimal;

import dev.gymbro.common.jpa.TimestampedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One planned exercise within a {@link WorkoutTemplate}: which exercise, in what
 * position, and the set/rep/RPE target. {@code (template_id, order_index)} is
 * unique.
 */
@Entity
@Table(name = "template_exercise")
public class TemplateExercise extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "target_sets", nullable = false)
    private int targetSets;

    @Column(name = "target_reps", nullable = false)
    private int targetReps;

    @Column(name = "target_rpe", precision = 3, scale = 1)
    private BigDecimal targetRpe;

    public Long getId() {
        return id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(Long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public int getTargetSets() {
        return targetSets;
    }

    public void setTargetSets(int targetSets) {
        this.targetSets = targetSets;
    }

    public int getTargetReps() {
        return targetReps;
    }

    public void setTargetReps(int targetReps) {
        this.targetReps = targetReps;
    }

    public BigDecimal getTargetRpe() {
        return targetRpe;
    }

    public void setTargetRpe(BigDecimal targetRpe) {
        this.targetRpe = targetRpe;
    }
}
