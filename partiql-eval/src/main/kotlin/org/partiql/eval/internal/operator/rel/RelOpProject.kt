package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.Row
import org.partiql.eval.operator.Expression
import org.partiql.eval.operator.Relation

internal class RelOpProject(
    private val input: Relation,
    private val projections: List<Expression>
) : Relation {

    private lateinit var env: Environment

    override fun open(env: Environment) {
        this.env = env
        input.open(env)
    }

    override fun hasNext(): Boolean {
        return input.hasNext()
    }

    override fun next(): Row {
        val r = input.next()
        val p = projections.map { it.eval(env.push(r)) }.toTypedArray()
        return Row(p)
    }

    override fun close() {
        input.close()
    }
}
