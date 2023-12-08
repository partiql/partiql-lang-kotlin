package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
internal class RelProject(
    private val input: Operator.Relation,
    private val projections: List<Operator.Expr>
) : Operator.Relation {

    override fun open() {
        input.open()
    }

    override fun next(): Record? {
        while (true) {
            val r = input.next() ?: return null
            val p = projections.map { it.eval(r) }.toTypedArray()
            return Record(p)
        }
    }

    override fun close() {
        input.close()
    }
}
