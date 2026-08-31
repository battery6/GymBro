package dev.gymbro.auth;

/**
 * The authenticated principal placed in the {@code SecurityContext} by
 * {@link dev.gymbro.auth.jwt.JwtAuthenticationFilter}. Reachable in controllers
 * via {@code @AuthenticationPrincipal AuthUser}.
 */
public record AuthUser(Long id, String email) {
}
