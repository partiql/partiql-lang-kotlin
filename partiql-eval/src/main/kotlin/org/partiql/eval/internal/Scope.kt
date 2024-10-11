package org.partiql.eval.internal

import org.partiql.spi.value.Datum

/**
 * This class represents a variable environment scope.
 */
internal class Scope(
    private val bindings: Record,
    private val parent: Scope? = null
) {

    companion object {
        @JvmStatic
        val empty: Scope = Scope(Record.empty, null)
    }

    operator fun get(index: Int): Datum {
        try {
            return this.bindings[index]
        } catch (_: Throwable) {
            throw IllegalStateException("Received error when searching for binding at index $index. Current bindings are: $this.")
        }
    }

    fun getOrNull(index: Int): Datum? {
        return this.bindings.values.getOrNull(index)
    }

    internal fun next(): Scope? {
        return this.parent
    }

    /**
     * Returns a new [Scope] that contains the [record] and encloses the current [Scope]. This is used to:
     * 1. Pass on a [Record] from a Rel to a Rex. Consider `SELECT a + 1 FROM t`. The PROJECT would likely grab the input
     * record from the SCAN, [push] it into the current environment, and pass it to the Expr representing `a + 1`.
     * 2. Create a nested scope. Consider `SELECT 1 + (SELECT t1.a + t2.b FROM t2 LIMIT 1) FROM t1`. Since the inner
     * SELECT (ExprSubquery) is creating a "nested scope", it would invoke [push].
     *
     * Here are the general rules to follow:
     * 1. When evaluating Expressions from within a Relation, one should always use [push] to "push" onto the stack.
     * 2. When evaluating Relations from within an Expression, one should always use [push] to "push" onto the stack.
     * 3. When evaluating Expressions from within a Relation, there is no need to use [push].
     * 4. When evaluating Relations from within a Relation, one **might** want to use [push]. Consider the LATERAL JOIN, for instance.
     *
     * @see [org.partiql.eval.internal.operator.Operator.Expr]
     * @see [org.partiql.eval.internal.operator.Operator.Relation]
     * @see [org.partiql.eval.internal.operator.rex.ExprSubquery]
     */
    internal fun push(record: Record): Scope = Scope(record, this)

    override fun toString(): String {
        return when (parent) {
            null -> bindings.toString()
            else -> "$bindings --> $parent"
        }
    }
}
