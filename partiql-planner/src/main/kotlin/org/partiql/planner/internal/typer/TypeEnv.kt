package org.partiql.planner.internal.typer

import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Rex
import org.partiql.spi.catalog.Identifier

/**
 * TypeEnv represents the variables type environment (holds references to both locals and globals).
 */
internal class TypeEnv(
    private val globals: Env,
    val locals: Scope
) {

    /**
     * Search Algorithm (LOCALS_FIRST):
     * 1. Match Binding Name
     *   - Match Locals
     *   - Match Globals
     * 2. Match Nested Field
     *   - Match Locals
     * Search Algorithm (GLOBALS_FIRST):
     * 1. Match Binding Name
     *   - Match Globals
     *   - Match Locals
     * 2. Match Nested Field
     *   - Match Locals
     */
    fun resolve(identifier: Identifier, strategy: Strategy = Strategy.LOCAL): Rex? {
        return when (strategy) {
            Strategy.LOCAL -> {
                locals.resolveName(identifier)
                    ?: globals.resolveTable(identifier)
                    ?: locals.resolveField(identifier)
            }
            Strategy.GLOBAL -> {
                globals.resolveTable(identifier)
                    ?: locals.resolveName(identifier)
                    ?: locals.resolveField(identifier)
            }
        }
    }
}
