package dev.gymbro.gym.entity;

import dev.gymbro.common.jpa.TimestampedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A movement that can be performed in a workout. {@code createdBy == null} means
 * a system-seeded library exercise; a non-null value is a user's custom exercise
 * ("custom" is derived from this, not stored — see ADR-010).
 */
@Entity
@Table(name = "exercise")
public class Exercise extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(nullable = false)
    private String name;

    @Column
    private String equipment;

    @Column
    private String description;

    public boolean isCustom() {
        return createdBy != null;
    }

    public Long getId() {
        return id;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
