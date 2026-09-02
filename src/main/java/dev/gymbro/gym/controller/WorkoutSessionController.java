package dev.gymbro.gym.controller;

import java.time.LocalDate;
import java.util.List;

import dev.gymbro.auth.AuthUser;
import dev.gymbro.gym.dto.CreateSessionRequest;
import dev.gymbro.gym.dto.LogSetRequest;
import dev.gymbro.gym.dto.SessionDetailResponse;
import dev.gymbro.gym.dto.SessionResponse;
import dev.gymbro.gym.dto.SetEntryResponse;
import dev.gymbro.gym.dto.UpdateSessionRequest;
import dev.gymbro.gym.service.WorkoutSessionService;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for workout sessions and the sets logged against them: start,
 * browse (optionally by date range), inspect with sets, end / annotate, delete,
 * and add or remove sets.
 *
 * <p>{@code @Validated} is on the class so Bean Validation cascades into the
 * elements of the {@code addSets} array body; Spring MVC does not validate
 * collection elements on its own.
 */
@RestController
@RequestMapping("/api/sessions")
@Validated
public class WorkoutSessionController {

    private final WorkoutSessionService workoutSessionService;

    public WorkoutSessionController(WorkoutSessionService workoutSessionService) {
        this.workoutSessionService = workoutSessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(
            @AuthenticationPrincipal AuthUser principal,
            @Valid @RequestBody CreateSessionRequest request) {
        return workoutSessionService.create(principal.id(), request);
    }

    @GetMapping
    public List<SessionResponse> list(
            @AuthenticationPrincipal AuthUser principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return workoutSessionService.list(principal.id(), from, to);
    }

    @GetMapping("/{sessionId}")
    public SessionDetailResponse get(
            @AuthenticationPrincipal AuthUser principal,
            @PathVariable Long sessionId) {
        return workoutSessionService.get(principal.id(), sessionId);
    }

    @PatchMapping("/{sessionId}")
    public SessionResponse update(
            @AuthenticationPrincipal AuthUser principal,
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateSessionRequest request) {
        return workoutSessionService.update(principal.id(), sessionId, request);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthUser principal,
            @PathVariable Long sessionId) {
        workoutSessionService.delete(principal.id(), sessionId);
    }

    @PostMapping("/{sessionId}/sets")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SetEntryResponse> addSets(
            @AuthenticationPrincipal AuthUser principal,
            @PathVariable Long sessionId,
            // Body is always a JSON array (one set is an array of one); the inner
            // @Valid, together with @Validated on the class, validates each element.
            @RequestBody @Valid List<@Valid LogSetRequest> sets) {
        return workoutSessionService.addSets(principal.id(), sessionId, sets);
    }

    @DeleteMapping("/{sessionId}/sets/{setId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSet(
            @AuthenticationPrincipal AuthUser principal,
            @PathVariable Long sessionId,
            @PathVariable Long setId) {
        workoutSessionService.deleteSet(principal.id(), sessionId, setId);
    }
}
