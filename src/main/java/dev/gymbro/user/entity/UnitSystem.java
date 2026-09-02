package dev.gymbro.user.entity;

/**
 * A user's preferred unit system. This is a <em>display</em> preference only —
 * everything is stored canonically in metric (kg, g, kcal); clients convert for
 * presentation.
 */
public enum UnitSystem {
    METRIC,
    IMPERIAL
}
