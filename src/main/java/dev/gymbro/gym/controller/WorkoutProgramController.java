package dev.gymbro.gym.controller;

import java.util.List;

import dev.gymbro.auth.AuthUser;
import dev.gymbro.gym.dto.AddProgramTemplateRequest;
import dev.gymbro.gym.dto.CreateWorkoutProgramRequest;
import dev.gymbro.gym.dto.ProgramTemplateResponse;
import dev.gymbro.gym.dto.WorkoutProgramDetailResponse;
import dev.gymbro.gym.dto.WorkoutProgramResponse;
import dev.gymbro.gym.service.WorkoutProgramService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/programs")
public class WorkoutProgramController {

    private final WorkoutProgramService workoutProgramService;

    public WorkoutProgramController(WorkoutProgramService workoutProgramService) {
        this.workoutProgramService = workoutProgramService;
    }

    @GetMapping
    public List<WorkoutProgramResponse> list(@AuthenticationPrincipal AuthUser principal) {
        return workoutProgramService.list(principal.id());
    }

    @GetMapping("/{programId}")
    public WorkoutProgramDetailResponse get(
            @AuthenticationPrincipal AuthUser principal,
            @PathVariable Long programId) {
        return workoutProgramService.get(principal.id(), programId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutProgramResponse create(
            @AuthenticationPrincipal AuthUser principal,
            @Valid @RequestBody CreateWorkoutProgramRequest request) {
        return workoutProgramService.create(principal.id(), request);
    }

    @PutMapping("/{programId}")
    public WorkoutProgramResponse update(
            @AuthenticationPrincipal AuthUser principal,
            @PathVariable Long programId,
            @Valid @RequestBody CreateWorkoutProgramRequest request) {
        return workoutProgramService.update(principal.id(), programId, request);
    }

    @DeleteMapping("/{programId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthUser principal,
            @PathVariable Long programId) {
        workoutProgramService.delete(principal.id(), programId);
    }

    @PostMapping("/{programId}/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramTemplateResponse addTemplate(
            @AuthenticationPrincipal AuthUser principal,
            @PathVariable Long programId,
            @Valid @RequestBody AddProgramTemplateRequest request) {
        return workoutProgramService.addTemplate(principal.id(), programId, request);
    }
}
