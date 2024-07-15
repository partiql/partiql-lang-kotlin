package org.partiql.planner.internal.typer

import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Rex
import org.partiql.spi.BindingPath

/**
 * TypeEnv represents the variables type environment (holds references to both locals and globals).
 */
internal class TypeEnv(
    val globals: Env,
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
    fun resolve(path: BindingPath, strategy: Strategy = Strategy.LOCAL): Rex? {
        return when (strategy) {
            Strategy.LOCAL -> locals.resolveName(path) ?: globals.resolveObj(path) ?: locals.resolveField(path)
            Strategy.GLOBAL -> globals.resolveObj(path) ?: locals.resolveName(path) ?: locals.resolveField(path)
        }
    }
}
