package dev.gymbro.gym.controller;

import java.util.List;

import dev.gymbro.auth.AuthUser;
import dev.gymbro.gym.dto.CreateExerciseRequest;
import dev.gymbro.gym.dto.ExerciseResponse;
import dev.gymbro.gym.service.ExerciseService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public List<ExerciseResponse> list(@AuthenticationPrincipal AuthUser principal) {
        return exerciseService.list(principal.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseResponse create(
            @AuthenticationPrincipal AuthUser principal,
            @Valid @RequestBody CreateExerciseRequest request) {
        return exerciseService.create(principal.id(), request);
    }

    @DeleteMapping("/{exerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthUser principal,
            @PathVariable Long exerciseId) {
        exerciseService.delete(principal.id(), exerciseId);
    }
}
