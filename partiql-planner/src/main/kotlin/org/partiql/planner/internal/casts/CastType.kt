package org.partiql.planner.internal.casts

/**
 * An COERCION will be inserted by the compiler during function resolution, an EXPLICIT CAST will never be inserted.
 *
 * COERCION: Lossless CAST(V AS T) -> T
 * EXPLICIT: Lossy    CAST(V AS T) -> T
 * UNSAFE:            CAST(V AS T) -> T|MISSING
 */
internal enum class CastType { COERCION, EXPLICIT, UNSAFE }
