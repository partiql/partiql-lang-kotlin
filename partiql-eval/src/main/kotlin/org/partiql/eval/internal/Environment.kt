package org.partiql.eval.internal

import java.util.Stack

/**
 * This class represents the Variables Environment defined in the PartiQL Specification. It differs slightly as it does
 * not hold the "current" [Record]. The reason for this is that the PartiQL Maintainers have opted to use the Volcano
 * Model for query execution (see [org.partiql.eval.internal.operator.Operator.Relation.next] and
 * [org.partiql.eval.internal.operator.Operator.Expr.eval]), however, the use of the [Environment] is to provide the
 * functionality defined in the PartiQL Specification. It accomplishes this by wrapping the "outer" variables
 * environments (or [scopes]).
 */
internal class Environment {

    private val scopes: Stack<Record> = Stack<Record>()

    /**
     * Creates a new scope using the [record] to execute the [block]. Pops the [record] once the [block] is done executing.
     */
    internal inline fun <T> scope(record: Record, block: () -> T): T {
        scopes.push(record)
        try {
            return block.invoke()
        } catch (t: Throwable) {
            throw t
        } finally {
            scopes.pop()
        }
    }

    /**
     * Gets the scope/record/variables-environment at the requested [depth].
     */
    operator fun get(depth: Int): Record {
        val index = scopes.lastIndex - depth
        return scopes[index]
    }
}
