package org.partiql.planner.internal

import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * Context parameter for the [PathResolverFn]. A simple typealias suffices for now.
 */
@OptIn(PartiQLValueExperimental::class)
internal typealias FnArgs = List<PartiQLValueType>
