package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelProject(
    private val input: Operator.Relation,
    private val projections: List<Operator.Expr>
) : Operator.Relation {

    private lateinit var env: Environment

    override fun open(env: Environment) {
        this.env = env
        input.open(env)
    }

    override fun hasNext(): Boolean {
        return input.hasNext()
    }

    override fun next(): Record {
        val r = input.next()
        val p = projections.map { it.eval(env.push(r)) }.toTypedArray()
        return Record(p)
    }

    override fun close() {
        input.close()
    }
}
