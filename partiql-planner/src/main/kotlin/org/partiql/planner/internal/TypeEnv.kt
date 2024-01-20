package org.partiql.planner.internal

import org.partiql.planner.internal.ir.Rel

/**
 * TypeEnv is represents the PartiQL local variable bindings.
 */
internal typealias TypeEnv = List<Rel.Binding>

internal fun TypeEnv.debug() = "< " + this.joinToString { "${it.name}: ${it.type}" } + " >"
