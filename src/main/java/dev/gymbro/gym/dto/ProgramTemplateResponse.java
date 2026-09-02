package dev.gymbro.gym.dto;

import dev.gymbro.gym.entity.ProgramTemplate;

public record ProgramTemplateResponse(
        Long id,
        Long templateId,
        String templateName,
        int orderIndex) {

    public static ProgramTemplateResponse from(ProgramTemplate programTemplate, String templateName) {
        return new ProgramTemplateResponse(
                programTemplate.getId(),
                programTemplate.getTemplateId(),
                templateName,
                programTemplate.getOrderIndex());
    }
}
