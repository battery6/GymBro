package dev.gymbro.user.dto;

import dev.gymbro.user.entity.UnitSystem;
import dev.gymbro.user.entity.User;

public record MeResponse(
        Long id,
        String email,
        String displayName,
        String timezone,
        UnitSystem unitSystem) {

    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getTimezone(),
                user.getUnitSystem());
    }
}
