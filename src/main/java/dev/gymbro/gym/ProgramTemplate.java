package dev.gymbro.gym;

import dev.gymbro.common.jpa.TimestampedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One slot in a {@link WorkoutProgram}: a {@link WorkoutTemplate} at a given
 * position. {@code (program_id, order_index)} is unique.
 */
@Entity
@Table(name = "program_template")
public class ProgramTemplate extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "program_id", nullable = false)
    private Long programId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public Long getId() {
        return id;
    }

    public Long getProgramId() {
        return programId;
    }

    public void setProgramId(Long programId) {
        this.programId = programId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
