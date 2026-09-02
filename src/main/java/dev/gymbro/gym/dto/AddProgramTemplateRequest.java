package dev.gymbro.gym.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Appends a template slot to a program. The same template may be added more than
 * once (at different positions); the new slot takes the next {@code orderIndex}.
 */
public record AddProgramTemplateRequest(
        @NotNull Long templateId) {
}
