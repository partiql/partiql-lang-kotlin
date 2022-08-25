package org.partiql.lang.eval

/**
 * Result of an evaluated PartiQLStatement
 */
sealed class PartiQLResult {

    class Value(val value: ExprValue) : PartiQLResult()

    class Insert(
        val target: String,
        val rows: Iterable<ExprValue>
    ) : PartiQLResult()

    class Delete(
        val target: String,
        val rows: Iterable<ExprValue>
    ) : PartiQLResult()
}
