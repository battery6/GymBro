package dev.gymbro.gym;

import java.time.Instant;
import java.time.LocalDate;

import dev.gymbro.common.jpa.TimestampedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A performed (or in-progress) workout. {@code templateId} is optional — sessions
 * can be freeform. A session is complete once {@code endTime} is set.
 * {@code atDate} is the calendar day the session belongs to, stored explicitly
 * (ADR-002) and set from the user's local day.
 */
@Entity
@Table(name = "workout_session")
public class WorkoutSession extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "at_date", nullable = false)
    private LocalDate atDate;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column
    private String notes;

    public boolean isComplete() {
        return endTime != null;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public LocalDate getAtDate() {
        return atDate;
    }

    public void setAtDate(LocalDate atDate) {
        this.atDate = atDate;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
