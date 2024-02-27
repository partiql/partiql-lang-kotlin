package org.partiql.eval.internal

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * This class represents the Variables Environment defined in the PartiQL Specification.
 */
internal class Environment(
    private val current: Record,
    private val parent: Environment? = null
) {

    companion object {
        @JvmStatic
        val empty: Environment = Environment(Record.empty, null)
    }

    @OptIn(PartiQLValueExperimental::class)
    operator fun get(index: Int): PartiQLValue {
        return this.current[index]
    }

    @OptIn(PartiQLValueExperimental::class)
    fun getOrNull(index: Int): PartiQLValue? {
        return this.current.values.getOrNull(index)
    }

    internal fun next(): Environment? {
        return this.parent
    }

    /**
     * Returns a new [Environment] that contains the [record] and encloses the current [Environment]. This is used to:
     * 1. Pass on a [Record] from a Rel to a Rex. Consider `SELECT a + 1 FROM t`. The PROJECT would likely grab the input
     * record from the SCAN, [nest] it into the current environment, and pass it to the Expr representing `a + 1`.
     * 2. Create a nested scope. Consider `SELECT 1 + (SELECT t1.a + t2.b FROM t2 LIMIT 1) FROM t1`. Since the inner
     * SELECT (ExprSubquery) is creating a "nested scope", it would invoke [nest].
     *
     * Here are the general rules to follow:
     * 1. When evaluating Expressions from within a Relation, one should always use [nest] to "push" onto the stack.
     * 2. When evaluating Relations from within an Expression, one should always use [nest] to "push" onto the stack.
     * 3. When evaluating Expressions from within a Relation, there is no need to use [nest].
     * 4. When evaluating Relations from within a Relation, one **might** want to use [nest]. Consider the LATERAL JOIN, for instance.
     *
     * @see [org.partiql.eval.internal.operator.Operator.Expr]
     * @see [org.partiql.eval.internal.operator.Operator.Relation]
     * @see [org.partiql.eval.internal.operator.rex.ExprSubquery]
     */
    internal fun nest(record: Record): Environment = Environment(
        record,
        this
    )

    override fun toString(): String {
        return when (parent) {
            null -> current.toString()
            else -> "$current --> $parent"
        }
    }
}
