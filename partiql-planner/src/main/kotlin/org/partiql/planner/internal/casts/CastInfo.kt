package org.partiql.planner.internal.casts

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature

/**
 * Each edge represents a type relationship
 */
@OptIn(FnExperimental::class)
internal data class CastInfo(
    val castType: CastType,
    val castFn: FnSignature.Scalar,
)
