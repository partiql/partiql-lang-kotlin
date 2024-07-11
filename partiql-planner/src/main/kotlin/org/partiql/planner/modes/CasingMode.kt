package org.partiql.planner.modes

/**
 * Identifier lookup case handling.
 */
public enum class CasingMode {
    NORMALIZE_LOWER,
    NORMALIZE_UPPER,
    INSENSITIVE,
    SENSITIVE,
}
