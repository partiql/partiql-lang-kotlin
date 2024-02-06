package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.listValue

/**
 * Invoke the constructor over all inputs.
 *
 * @property input
 * @property constructor
 */
internal class ExprSelect(
    val input: Operator.Relation,
    val constructor: Operator.Expr,
    val ordered: Boolean
) : Operator.Expr {

    /**
     * @param record
     * @return
     */
    @PartiQLValueExperimental
    override fun eval(record: Record): PartiQLValue {
        val elements = mutableListOf<PartiQLValue>()
        input.open()
        while (true) {
            val r = input.next() ?: break
            val e = constructor.eval(r)
            elements.add(e)
        }
        input.close()
        return when (ordered) {
            true -> listValue(elements)
            false -> bagValue(elements)
        }
    }
}
