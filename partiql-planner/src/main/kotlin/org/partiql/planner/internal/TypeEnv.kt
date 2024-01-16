package org.partiql.planner.internal

import org.partiql.planner.internal.ir.Rel

/**
 * TypeEnv represents the environment in which we type expressions and resolve variables while planning.
 *
 * TODO TypeEnv should be a stack of locals; also the strategy has been kept here because it's easier to
 *  pass through the traversal like this, but is conceptually odd to associate with the TypeEnv.
 * @property schema
 * @property strategy
 */
internal class TypeEnv(
    val schema: List<Rel.Binding>,
    val strategy: ResolutionStrategy,
) {

    /**
     * Return a copy with GLOBAL lookup strategy
     */
    fun global() = TypeEnv(schema, ResolutionStrategy.GLOBAL)

    /**
     * Return a copy with LOCAL lookup strategy
     */
    fun local() = TypeEnv(schema, ResolutionStrategy.LOCAL)

    /**
     * Debug string
     */
    override fun toString() = buildString {
        append("(")
        append("strategy=$strategy")
        append(", ")
        val bindings = "< " + schema.joinToString { "${it.name}: ${it.type}" } + " >"
        append("bindings=$bindings")
        append(")")
    }
}
