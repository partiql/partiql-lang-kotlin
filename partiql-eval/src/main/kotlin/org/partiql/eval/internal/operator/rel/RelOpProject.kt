package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Row
import org.partiql.eval.internal.operator.Operator

internal class RelOpProject(
    private val env: Environment,
    private val input: Operator.Relation,
    private val projections: List<Operator.Expr>,
) : Operator.Relation {

    override fun open() {
        input.open()
    }

    override fun hasNext(): Boolean {
        return input.hasNext()
    }

    override fun next(): Row {
        val curr = input.next()
        env.push(curr)
        val next = Array(projections.size) { i -> projections[i].eval() }
        env.pop()
        return Row(next)
    }

    override fun close() {
        input.close()
    }
}
