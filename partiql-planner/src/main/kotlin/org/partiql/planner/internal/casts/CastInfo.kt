package org.partiql.planner.internal.casts

import org.partiql.planner.internal.ir.Ref

/**
 * Each edge represents a type relationship
 */
internal data class CastInfo(val type: CastType, val ref: Ref.Cast)
