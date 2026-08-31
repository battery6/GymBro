package dev.gymbro.user;

public record MeResponse(
        Long id,
        String email,
        String displayName,
        String timezone,
        UnitSystem unitSystem) {

    static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getTimezone(),
                user.getUnitSystem());
    }
}
