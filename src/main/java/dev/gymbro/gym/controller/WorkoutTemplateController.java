package dev.gymbro.gym.controller;

import dev.gymbro.auth.AuthUser;
import dev.gymbro.gym.dto.AddTemplateExerciseRequest;
import dev.gymbro.gym.dto.CreateWorkoutTemplateRequest;
import dev.gymbro.gym.dto.TemplateExerciseResponse;
import dev.gymbro.gym.dto.WorkoutTemplateResponse;
import dev.gymbro.gym.service.WorkoutTemplateService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/templates")
public class WorkoutTemplateController {

    private final WorkoutTemplateService workoutTemplateService;

    public WorkoutTemplateController(WorkoutTemplateService workoutTemplateService) {
        this.workoutTemplateService = workoutTemplateService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutTemplateResponse create(
            @AuthenticationPrincipal AuthUser principal,
            @Valid @RequestBody CreateWorkoutTemplateRequest request) {
        return workoutTemplateService.create(principal.id(), request);
    }

    @PostMapping("/{templateId}/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateExerciseResponse addExercise(
            @AuthenticationPrincipal AuthUser principal,
            @PathVariable Long templateId,
            @Valid @RequestBody AddTemplateExerciseRequest request) {
        return workoutTemplateService.addExercise(principal.id(), templateId, request);
    }
}
